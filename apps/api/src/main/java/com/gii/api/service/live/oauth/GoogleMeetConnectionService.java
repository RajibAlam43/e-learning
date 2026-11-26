package com.gii.api.service.live.oauth;

import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.entity.integration.GoogleMeetConnection;
import com.gii.common.entity.integration.GoogleMeetOauthState;
import com.gii.common.repository.integration.GoogleMeetConnectionRepository;
import com.gii.common.repository.integration.GoogleMeetOauthStateRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class GoogleMeetConnectionService {
  public static final String COOKIE = "google_meet_oauth";
  public static final String CALLBACK = "/public/google-meet/oauth/callback";
  public static final String MEET_SCOPE = "https://www.googleapis.com/auth/meetings.space.created";
  public static final String CALENDAR_SCOPE =
      "https://www.googleapis.com/auth/calendar.events.owned";
  private final GoogleMeetConnectionRepository connections;
  private final GoogleMeetOauthStateRepository states;
  private final GoogleMeetTokenCipherService cipher;
  private final GoogleMeetOauthClientService oauth;
  private final CurrentUserService users;
  private final TransactionTemplate transactions;

  @Value("${integrations.google-oauth.client-id:}")
  private String clientId;

  @Value("${integrations.google-meet.oauth-redirect-uri:}")
  private String redirectUri;

  @Value("${integrations.google-meet.oauth-return-url:}")
  private String returnUrl;

  @Value("${integrations.google-meet.oauth-cookie-secure:true}")
  private boolean cookieSecure;

  @Value("${integrations.google-meet.shared-organizer-enabled:false}")
  private boolean shared;

  @Value("${integrations.google-meet.calendar-sync-enabled:true}")
  private boolean calendar;

  public Status status(Authentication auth) {
    var connection = connections.findByUserId(users.getCurrentUserId(auth)).orElse(null);
    boolean valid =
        connection != null
            && (connection.getRefreshExpiresAt() == null
                || connection.getRefreshExpiresAt().isAfter(Instant.now()))
            && scopes(connection.getGrantedScopes()).contains(requiredScope());
    return new Status(valid, connection == null ? null : connection.getEmail(), shared, calendar);
  }

  public Authorization start(Authentication auth, HttpServletResponse response) {
    cipher.requireConfigured();
    UUID userId = users.getCurrentUserId(auth);
    if (clientId.isBlank() || redirectUri.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Configure Google OAuth client and callback URL");
    }
    String state = GoogleMeetTokenCipherService.randomToken();
    String browser = GoogleMeetTokenCipherService.randomToken();
    String verifier = GoogleMeetTokenCipherService.randomToken();
    String nonce = GoogleMeetTokenCipherService.randomToken();
    Instant expires = Instant.now().plusSeconds(600);
    String requested = "openid email " + requiredScope();
    transactions.executeWithoutResult(status -> {
      states.deleteByExpiresAtBefore(Instant.now());
      states.save(
          GoogleMeetOauthState.builder()
              .userId(userId)
              .stateHash(GoogleMeetTokenCipherService.hash(state))
              .browserHash(GoogleMeetTokenCipherService.hash(browser))
              .nonceHash(GoogleMeetTokenCipherService.hash(nonce))
              .encryptedVerifier(cipher.encrypt(verifier, "state:" + userId))
              .requestedScopes(requested)
              .expiresAt(expires)
              .build());
    });
    cookie(response, browser, 600);
    String url =
        UriComponentsBuilder.fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
            .queryParam("client_id", clientId)
            .queryParam("redirect_uri", redirectUri)
            .queryParam("response_type", "code")
            .queryParam("scope", requested)
            .queryParam("state", state)
            .queryParam("nonce", nonce)
            .queryParam("code_challenge", GoogleMeetTokenCipherService.hash(verifier))
            .queryParam("code_challenge_method", "S256")
            .queryParam("access_type", "offline")
            .queryParam("include_granted_scopes", true)
            .queryParam("prompt", "consent select_account")
            .build()
            .encode()
            .toUriString();
    return new Authorization(url, expires);
  }

  public void complete(String state, String code, String error, String browser) {
    if (state == null || state.length() > 128 || browser == null || browser.length() > 128) {
      throw denied();
    }
    GoogleMeetOauthState attempt =
        transactions.execute(status -> {
          var entry =
              states.findLocked(GoogleMeetTokenCipherService.hash(state)).orElseThrow(this::denied);
          if (entry.getConsumedAt() != null
              || !entry.getExpiresAt().isAfter(Instant.now())
              || !MessageDigest.isEqual(
                  entry.getBrowserHash().getBytes(StandardCharsets.UTF_8),
                  GoogleMeetTokenCipherService.hash(browser).getBytes(StandardCharsets.UTF_8))) {
            throw denied();
          }
          entry.setConsumedAt(Instant.now());
          return entry;
        });
    if (attempt == null || error != null || code == null || code.length() > 4096) {
      throw denied();
    }
    var grant =
        oauth.exchange(
            code,
            cipher.decrypt(attempt.getEncryptedVerifier(), "state:" + attempt.getUserId()),
            attempt.getNonceHash());
    String required =
        scopes(attempt.getRequestedScopes()).stream()
            .filter(scope -> scope.startsWith("https://www.googleapis.com/auth/"))
            .findFirst()
            .orElseThrow();
    if (grant.scope() == null
        || !scopes(grant.scope()).contains(required)
        || grant.refreshToken() == null
        || grant.refreshToken().isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Google permission was incomplete. Reconnect and approve the requested access.");
    }
    transactions.executeWithoutResult(status -> {
      var connection =
          connections
              .findByUserId(attempt.getUserId())
              .orElseGet(() -> GoogleMeetConnection.builder().userId(attempt.getUserId()).build());
      connection.setGoogleSubject(grant.subject());
      connection.setEmail(grant.email());
      connection.setEncryptedRefreshToken(
          cipher.encrypt(grant.refreshToken(), "refresh:" + attempt.getUserId()));
      connection.setGrantedScopes(grant.scope());
      connection.setRefreshExpiresAt(grant.refreshExpiresAt());
      connections.saveAndFlush(connection);
    });
  }

  public Credential credential(UUID userId, String expectedSubject, String requiredScope) {
    var connection =
        connections
            .findByUserId(userId)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The primary teacher must connect Google before this class can be scheduled"));
    if ((expectedSubject != null && !expectedSubject.equals(connection.getGoogleSubject()))
        || (connection.getRefreshExpiresAt() != null
            && !connection.getRefreshExpiresAt().isAfter(Instant.now()))
        || !scopes(connection.getGrantedScopes()).contains(requiredScope)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Reconnect the original meeting owner's Google account with the required permission");
    }
    return new Credential(
        oauth.refresh(cipher.decrypt(connection.getEncryptedRefreshToken(), "refresh:" + userId)),
        userId,
        connection.getGoogleSubject(),
        connection.getEmail());
  }

  public void disconnect(Authentication auth) {
    // Delete locally; the teacher can revoke the app's Google grant in their Google account
    // settings.
    transactions.executeWithoutResult(
        status -> connections.deleteByUserId(users.getCurrentUserId(auth)));
  }

  public String returnUrl(boolean success) {
    if (returnUrl.isBlank()) {
      return "";
    }
    return UriComponentsBuilder.fromUriString(returnUrl)
        .queryParam("googleMeet", success ? "connected" : "failed")
        .build()
        .encode()
        .toUriString();
  }

  public void clearCookie(HttpServletResponse response) {
    cookie(response, "", 0);
  }

  private void cookie(HttpServletResponse response, String value, int seconds) {
    response.addHeader(
        HttpHeaders.SET_COOKIE,
        ResponseCookie.from(COOKIE, value)
            .path(CALLBACK)
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite("Lax")
            .maxAge(seconds)
            .build()
            .toString());
  }

  private String requiredScope() {
    return calendar ? CALENDAR_SCOPE : MEET_SCOPE;
  }

  private List<String> scopes(String value) {
    return Arrays.stream(value.trim().split("\\s+")).toList();
  }

  private ResponseStatusException denied() {
    return new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Google connection expired, was denied or belongs to another browser. Start again.");
  }

  @Builder
  public record Status(
      boolean connected,
      String email,
      boolean sharedOrganizerEnabled,
      boolean calendarSyncEnabled) {}

  @Builder
  public record Authorization(String authorizationUrl, Instant expiresAt) {}

  @Builder
  public record Credential(
      @com.fasterxml.jackson.annotation.JsonIgnore String accessToken,
      UUID userId,
      String subject,
      String email) {}
}

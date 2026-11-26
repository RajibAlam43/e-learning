package com.gii.api.service.live.oauth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GoogleMeetOauthClientService {
  @Value("${integrations.google-oauth.client-id:}")
  private String clientId;

  @Value("${integrations.google-oauth.client-secret:}")
  private String clientSecret;

  @Value("${integrations.google-oauth.token-url:https://oauth2.googleapis.com/token}")
  private String tokenUrl;

  @Value("${integrations.google-meet.oauth-redirect-uri:}")
  private String redirectUri;

  public Grant exchange(String code, String verifier, String nonceHash) {
    try {
      var request =
          new GoogleAuthorizationCodeTokenRequest(
              new NetHttpTransport(),
              GsonFactory.getDefaultInstance(),
              clientId,
              clientSecret,
              code,
              redirectUri);
      request.setTokenServerUrl(new GenericUrl(tokenUrl));
      request.set("code_verifier", verifier);
      request.setRequestInitializer(http -> {
        http.setConnectTimeout(15000);
        http.setReadTimeout(15000);
      });
      var response = request.execute();
      var identity =
          new GoogleIdTokenVerifier.Builder(
                  new NetHttpTransport(), GsonFactory.getDefaultInstance())
              .setAudience(List.of(clientId))
              .build()
              .verify(response.getIdToken());
      if (identity == null
          || !Boolean.TRUE.equals(identity.getPayload().getEmailVerified())
          || identity.getPayload().getSubject() == null
          || identity.getPayload().getEmail() == null
          || !(identity.getPayload().get("nonce") instanceof String nonce)
          || !GoogleMeetTokenCipherService.hash(nonce).equals(nonceHash)) {
        throw new IllegalArgumentException("Invalid Google identity");
      }
      Object expiry = response.get("refresh_token_expires_in");
      Instant expires =
          expiry instanceof Number seconds ? Instant.now().plusSeconds(seconds.longValue()) : null;
      return new Grant(
          response.getAccessToken(),
          response.getRefreshToken(),
          response.getScope(),
          identity.getPayload().getSubject(),
          identity.getPayload().getEmail(),
          expires);
    } catch (Exception error) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY,
          "Google authorization could not be verified. Start the connection again.");
    }
  }

  public String refresh(String refreshToken) {
    try {
      var request =
          new GoogleRefreshTokenRequest(
              new NetHttpTransport(),
              GsonFactory.getDefaultInstance(),
              refreshToken,
              clientId,
              clientSecret);
      request.setTokenServerUrl(new GenericUrl(tokenUrl));
      request.setRequestInitializer(http -> {
        http.setConnectTimeout(15000);
        http.setReadTimeout(15000);
      });
      String token = request.execute().getAccessToken();
      if (token == null || token.isBlank()) {
        throw new IllegalArgumentException("Missing token");
      }
      return token;
    } catch (Exception error) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Google authorization is unavailable or expired."
              + " The meeting owner must reconnect Google.");
    }
  }

  @Builder
  public record Grant(
      @JsonIgnore String accessToken,
      @JsonIgnore String refreshToken,
      String scope,
      String subject,
      String email,
      Instant refreshExpiresAt) {}
}

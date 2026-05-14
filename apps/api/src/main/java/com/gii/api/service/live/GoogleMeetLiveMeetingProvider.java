package com.gii.api.service.live;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gii.common.enums.LiveClassProvider;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class GoogleMeetLiveMeetingProvider implements LiveMeetingProvider {
  private final WebClient.Builder webClientBuilder;

  @Value("${integrations.google-calendar.base-url}")
  private String baseUrl;

  @Value("${integrations.google-calendar.calendar-id}")
  private String calendarId;

  @Value("${integrations.google-oauth.token-url}")
  private String oauthTokenUrl;

  @Value("${integrations.google-oauth.client-id}")
  private String oauthClientId;

  @Value("${integrations.google-oauth.client-secret}")
  private String oauthClientSecret;

  @Value("${integrations.google-oauth.refresh-token}")
  private String oauthRefreshToken;

  @Override
  public LiveClassProvider provider() {
    return LiveClassProvider.GOOGLE_MEET;
  }

  @Override
  public LiveMeetingCreateResult create(LiveMeetingCreateRequest request) {
    if (oauthClientId.isBlank() || oauthClientSecret.isBlank() || oauthRefreshToken.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Google OAuth credentials are not configured");
    }
    String accessToken = issueAccessToken();

    String requestId = UUID.randomUUID().toString();
    GoogleCalendarEventResponse response =
        webClientBuilder
            .baseUrl(baseUrl)
            .build()
            .post()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path("/calendars/{calendarId}/events")
                        .queryParam("conferenceDataVersion", "1")
                        .build(calendarId))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                new GoogleCalendarEventRequest(
                    request.title(),
                    request.description(),
                    new DateTimeValue(request.startsAt().atOffset(ZoneOffset.UTC).toString(), "UTC"),
                    new DateTimeValue(request.endsAt().atOffset(ZoneOffset.UTC).toString(), "UTC"),
                    new ConferenceData(new CreateConferenceRequest(requestId, new ConferenceSolutionKey("hangoutsMeet")))))
            .retrieve()
            .bodyToMono(GoogleCalendarEventResponse.class)
            .block();

    if (response == null
        || response.hangoutLink() == null
        || response.conferenceData() == null
        || response.conferenceData().conferenceId() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Failed to create Google Meet link");
    }

    return LiveMeetingCreateResult.builder()
        .meetingId(response.conferenceData().conferenceId())
        .hostStartUrl(response.htmlLink())
        .participantJoinUrl(response.hangoutLink())
        .build();
  }

  private record GoogleCalendarEventRequest(
      String summary,
      String description,
      DateTimeValue start,
      DateTimeValue end,
      ConferenceData conferenceData) {}

  private record DateTimeValue(String dateTime, String timeZone) {}

  private record ConferenceData(CreateConferenceRequest createRequest) {}

  private record CreateConferenceRequest(String requestId, ConferenceSolutionKey conferenceSolutionKey) {}

  private record ConferenceSolutionKey(String type) {}

  private record GoogleCalendarEventResponse(
      String id, String htmlLink, String hangoutLink, ConferenceDataResponse conferenceData) {}

  private record ConferenceDataResponse(String conferenceId, List<EntryPoint> entryPoints) {}

  private record EntryPoint(String entryPointType, String uri) {}

  private String issueAccessToken() {
    String formBody =
        "client_id="
            + urlEncode(oauthClientId)
            + "&client_secret="
            + urlEncode(oauthClientSecret)
            + "&refresh_token="
            + urlEncode(oauthRefreshToken)
            + "&grant_type=refresh_token";

    GoogleOAuthTokenResponse response =
        webClientBuilder
            .build()
            .post()
            .uri(oauthTokenUrl)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue(formBody)
            .retrieve()
            .bodyToMono(GoogleOAuthTokenResponse.class)
            .block();

    if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Failed to refresh Google OAuth access token");
    }
    return response.accessToken();
  }

  private String urlEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private record GoogleOAuthTokenResponse(@JsonProperty("access_token") String accessToken) {}
}

package com.gii.api.service.live;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.charset.StandardCharsets;
import com.gii.common.enums.LiveClassProvider;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Base64;
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
public class ZoomLiveMeetingProvider implements LiveMeetingProvider {
  private final WebClient.Builder webClientBuilder;

  @Value("${integrations.zoom.base-url}")
  private String baseUrl;

  @Value("${integrations.zoom.oauth-token-url}")
  private String oauthTokenUrl;

  @Value("${integrations.zoom.account-id}")
  private String accountId;

  @Value("${integrations.zoom.client-id}")
  private String clientId;

  @Value("${integrations.zoom.client-secret}")
  private String clientSecret;

  @Value("${integrations.zoom.host-user-id}")
  private String hostUserId;

  @Override
  public LiveClassProvider provider() {
    return LiveClassProvider.ZOOM;
  }

  @Override
  public LiveMeetingCreateResult create(LiveMeetingCreateRequest request) {
    if (accountId.isBlank() || clientId.isBlank() || clientSecret.isBlank() || hostUserId.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Zoom OAuth credentials are not configured");
    }
    String accessToken = issueAccessToken();

    long durationMinutes = Math.max(1, Duration.between(request.startsAt(), request.endsAt()).toMinutes());
    ZoomCreateMeetingResponse response =
        webClientBuilder
            .baseUrl(baseUrl)
            .build()
            .post()
            .uri("/users/{userId}/meetings", hostUserId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                new ZoomCreateMeetingRequest(
                    request.title(),
                    request.description(),
                    request.startsAt().atOffset(ZoneOffset.UTC).toString(),
                    durationMinutes,
                    2,
                    "UTC"))
            .retrieve()
            .bodyToMono(ZoomCreateMeetingResponse.class)
            .block();

    if (response == null || response.id() == null || response.join_url() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Failed to create Zoom meeting");
    }

    return LiveMeetingCreateResult.builder()
        .meetingId(String.valueOf(response.id()))
        .hostStartUrl(response.start_url())
        .participantJoinUrl(response.join_url())
        .build();
  }

  @Override
  public void update(LiveMeetingUpdateRequest request) {
    String accessToken = issueAccessToken();
    long durationMinutes = Math.max(1, Duration.between(request.startsAt(), request.endsAt()).toMinutes());
    webClientBuilder
        .baseUrl(baseUrl)
        .build()
        .patch()
        .uri("/meetings/{meetingId}", request.providerMeetingId())
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            new ZoomCreateMeetingRequest(
                request.title(),
                request.description(),
                request.startsAt().atOffset(ZoneOffset.UTC).toString(),
                durationMinutes,
                2,
                "UTC"))
        .retrieve()
        .toBodilessEntity()
        .block();
  }

  @Override
  public void cancel(LiveMeetingCancelRequest request) {
    String accessToken = issueAccessToken();
    webClientBuilder
        .baseUrl(baseUrl)
        .build()
        .delete()
        .uri("/meetings/{meetingId}", request.providerMeetingId())
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
        .retrieve()
        .toBodilessEntity()
        .block();
  }

  private record ZoomCreateMeetingRequest(
      String topic, String agenda, String start_time, long duration, Integer type, String timezone) {}

  private record ZoomCreateMeetingResponse(Long id, String start_url, String join_url) {}

  private String issueAccessToken() {
    String basicAuth =
        Base64.getEncoder()
            .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

    ZoomOAuthTokenResponse response =
        webClientBuilder
            .build()
            .post()
            .uri(
                oauthTokenUrl
                    + "?grant_type=account_credentials&account_id="
                    + java.net.URLEncoder.encode(accountId, java.nio.charset.StandardCharsets.UTF_8))
            .header(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
            .retrieve()
            .bodyToMono(ZoomOAuthTokenResponse.class)
            .block();

    if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Failed to issue Zoom OAuth access token");
    }
    return response.accessToken();
  }

  private record ZoomOAuthTokenResponse(@JsonProperty("access_token") String accessToken) {}
}

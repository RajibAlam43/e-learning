package com.gii.api.service.media;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class MuxDirectUploadClient {

  private static final int MIN_UPLOAD_TIMEOUT_SECONDS = 60;
  private static final int MAX_UPLOAD_TIMEOUT_SECONDS = 604800;

  private final WebClient webClient;
  private final String corsOrigin;
  private final int timeoutSeconds;
  private final String videoQuality;
  private final Duration apiTimeout;

  public MuxDirectUploadClient(
      WebClient.Builder webClientBuilder,
      @Value("${mux.api-base-url:https://api.mux.com}") String apiBaseUrl,
      @Value("${mux.token-id}") String tokenId,
      @Value("${mux.token-secret}") String tokenSecret,
      @Value("${mux.direct-upload-cors-origin:*}") String corsOrigin,
      @Value("${mux.direct-upload-timeout-seconds:3600}") int timeoutSeconds,
      @Value("${mux.api-timeout-seconds:10}") int apiTimeoutSeconds,
      @Value("${mux.video-quality:basic}") String videoQuality) {
    this.webClient =
        webClientBuilder
            .clone()
            .baseUrl(apiBaseUrl)
            .defaultHeaders(headers -> headers.setBasicAuth(tokenId, tokenSecret))
            .build();
    if (timeoutSeconds < MIN_UPLOAD_TIMEOUT_SECONDS
        || timeoutSeconds > MAX_UPLOAD_TIMEOUT_SECONDS) {
      throw new IllegalStateException("Mux direct upload timeout must be between 60 and 604800");
    }
    if (apiTimeoutSeconds <= 0) {
      throw new IllegalStateException("Mux API timeout must be positive");
    }
    this.corsOrigin = corsOrigin;
    this.timeoutSeconds = timeoutSeconds;
    this.videoQuality = videoQuality;
    this.apiTimeout = Duration.ofSeconds(apiTimeoutSeconds);
  }

  public DirectUpload create(String lessonId, String title) {
    Map<String, Object> assetSettings =
        Map.of(
            "playback_policies",
            List.of("signed"),
            "passthrough",
            lessonId,
            "video_quality",
            videoQuality,
            "meta",
            Map.of("title", title, "external_id", lessonId));
    Map<String, Object> request =
        Map.of(
            "cors_origin", corsOrigin,
            "timeout", timeoutSeconds,
            "new_asset_settings", assetSettings);
    try {
      DirectUploadEnvelope envelope =
          webClient
              .post()
              .uri("/video/v1/uploads")
              .bodyValue(request)
              .retrieve()
              .bodyToMono(DirectUploadEnvelope.class)
              .block(apiTimeout);
      if (envelope == null || envelope.data() == null || !isValidUpload(envelope.data())) {
        throw muxUnavailable();
      }
      return envelope.data();
    } catch (ResponseStatusException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw muxUnavailable();
    }
  }

  private boolean isValidUpload(DirectUpload upload) {
    if (upload.id() == null
        || upload.id().isBlank()
        || upload.url() == null
        || upload.url().isBlank()
        || (upload.timeout() != null
            && (upload.timeout() < MIN_UPLOAD_TIMEOUT_SECONDS
                || upload.timeout() > MAX_UPLOAD_TIMEOUT_SECONDS))) {
      return false;
    }
    try {
      URI uri = URI.create(upload.url());
      return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null;
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }

  private ResponseStatusException muxUnavailable() {
    return new ResponseStatusException(
        HttpStatus.BAD_GATEWAY, "Mux did not create a direct upload URL");
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record DirectUploadEnvelope(DirectUpload data) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record DirectUpload(
      String id,
      String url,
      Integer timeout,
      String status,
      @JsonProperty("asset_id") String assetId) {}
}

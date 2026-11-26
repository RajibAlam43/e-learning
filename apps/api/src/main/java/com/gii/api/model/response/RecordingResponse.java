package com.gii.api.model.response;

import com.gii.common.enums.RecordingMethod;
import com.gii.common.enums.RecordingStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record RecordingResponse(
    UUID id,
    RecordingMethod method,
    RecordingStatus status,
    Instant createdAt,
    Double durationSeconds,
    String failureMessage) {
  @Builder
  public record ClassRecordings(
      boolean enabled,
      String title,
      String meetingUrl,
      List<RecordingResponse> recordings,
      String classStatus) {}

  @Builder
  public record UploadSession(
      UUID id,
      String uploadToken,
      Instant expiresAt,
      String mimeType,
      long partSizeBytes,
      long maxBytes) {}

  @Builder
  public record PartUrl(String url) {}

  @Builder
  public record Playback(String url, String fileUrl, Instant expiresAt) {}
}

package com.gii.api.service.media;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gii.common.entity.course.MediaAsset;
import com.gii.common.entity.course.MuxVideoUpload;
import com.gii.common.enums.MediaAssetType;
import com.gii.common.enums.MediaProvider;
import com.gii.common.enums.MediaStatus;
import com.gii.common.enums.MuxUploadStatus;
import com.gii.common.enums.PlaybackMode;
import com.gii.common.enums.PlaybackPolicy;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.course.MediaAssetRepository;
import com.gii.common.repository.course.MuxVideoUploadRepository;
import com.gii.common.repository.course.MuxWebhookEventRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MuxWebhookService {

  private static final int MAX_ERROR_LENGTH = 2000;

  private final MuxWebhookSignatureVerifier signatureVerifier;
  private final ObjectMapper objectMapper;
  private final MuxVideoUploadRepository uploadRepository;
  private final MuxWebhookEventRepository eventRepository;
  private final MediaAssetRepository mediaAssetRepository;
  private final LessonRepository lessonRepository;

  @Transactional
  public void execute(String signatureHeader, String rawBody) {
    signatureVerifier.verify(signatureHeader, rawBody);
    JsonNode root = parse(rawBody);
    String eventId = requiredText(root, "id");
    String eventType = requiredText(root, "type");
    if (eventType.length() > 100) {
      throw badRequest("Invalid Mux event type");
    }
    if (eventRepository.claim(eventId, eventType) == 0) {
      return;
    }

    JsonNode data = root.path("data");
    switch (eventType) {
      case "video.upload.asset_created" -> handleAssetCreated(data);
      case "video.asset.ready" -> handleAssetReady(data);
      case "video.upload.errored", "video.upload.cancelled", "video.upload.timed_out" ->
          handleUploadFailed(data);
      case "video.asset.errored" -> handleAssetFailed(data);
      default -> {
        // Valid Mux event unrelated to direct video uploads.
      }
    }
  }

  private void handleAssetCreated(JsonNode data) {
    String uploadId = optionalText(data, "id");
    String assetId = optionalText(data, "asset_id");
    if (uploadId == null || assetId == null) {
      return;
    }
    uploadRepository
        .findByUploadId(uploadId)
        .filter(upload -> passthroughMatches(upload, data))
        .filter(upload -> upload.getAssetId() == null || assetId.equals(upload.getAssetId()))
        .ifPresent(
            upload -> {
              upload.setAssetId(assetId);
              upload.setStatus(MuxUploadStatus.PROCESSING);
              upload.setErrorMessage(null);
            });
  }

  private void handleAssetReady(JsonNode data) {
    Optional<MuxVideoUpload> match = findUploadForAssetEvent(data);
    if (match.isEmpty() || !passthroughMatches(match.get(), data) || !isLatest(match.get())) {
      return;
    }
    String assetId = optionalText(data, "id");
    String playbackId = signedPlaybackId(data);
    if (assetId == null
        || playbackId == null
        || (match.get().getAssetId() != null && !assetId.equals(match.get().getAssetId()))) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Mux ready event does not match the tracked signed asset");
    }

    MuxVideoUpload upload = match.get();
    upload.setAssetId(assetId);
    upload.setPlaybackId(playbackId);
    upload.setStatus(MuxUploadStatus.READY);
    upload.setErrorMessage(null);

    MediaAsset mediaAsset =
        mediaAssetRepository
            .findByLessonId(upload.getLesson().getId())
            .orElseGet(
                () ->
                    MediaAsset.builder()
                        .lesson(upload.getLesson())
                        .title(upload.getLesson().getTitle())
                        .build());
    mediaAsset.setProvider(MediaProvider.MUX);
    mediaAsset.setAssetType(MediaAssetType.VIDEO);
    mediaAsset.setProviderAssetId(assetId);
    mediaAsset.setPlaybackId(playbackId);
    mediaAsset.setPlaybackPolicy(PlaybackPolicy.SIGNED);
    mediaAsset.setPreferredPlaybackMode(PlaybackMode.HLS);
    mediaAsset.setStatus(MediaStatus.READY);
    mediaAsset.setMaxResolution(firstText(data, "resolution_tier", "max_stored_resolution"));
    Integer duration = durationSeconds(data);
    mediaAsset.setDurationSec(duration);
    mediaAssetRepository.save(mediaAsset);
    if (duration != null) {
      upload.getLesson().setDurationSeconds(duration);
      lessonRepository.save(upload.getLesson());
    }
  }

  private void handleUploadFailed(JsonNode data) {
    String uploadId = optionalText(data, "id");
    if (uploadId == null) {
      return;
    }
    uploadRepository
        .findByUploadId(uploadId)
        .filter(upload -> passthroughMatches(upload, data))
        .ifPresent(upload -> markFailed(upload, data));
  }

  private void handleAssetFailed(JsonNode data) {
    findUploadForAssetEvent(data)
        .filter(upload -> passthroughMatches(upload, data))
        .ifPresent(
            upload -> {
              markFailed(upload, data);
              mediaAssetRepository
                  .findByLessonId(upload.getLesson().getId())
                  .filter(
                      asset ->
                          asset.getProvider() == MediaProvider.MUX
                              && upload.getAssetId() != null
                              && upload.getAssetId().equals(asset.getProviderAssetId()))
                  .ifPresent(asset -> asset.setStatus(MediaStatus.FAILED));
            });
  }

  private Optional<MuxVideoUpload> findUploadForAssetEvent(JsonNode data) {
    String uploadId = optionalText(data, "upload_id");
    if (uploadId != null) {
      return uploadRepository.findByUploadId(uploadId);
    }
    String assetId = optionalText(data, "id");
    if (assetId != null) {
      Optional<MuxVideoUpload> byAsset = uploadRepository.findByAssetId(assetId);
      if (byAsset.isPresent()) {
        return byAsset;
      }
    }
    String passthrough = optionalText(data, "passthrough");
    try {
      return passthrough == null
          ? Optional.empty()
          : uploadRepository.findFirstByLessonIdOrderByCreatedAtDesc(UUID.fromString(passthrough));
    } catch (IllegalArgumentException exception) {
      return Optional.empty();
    }
  }

  private boolean passthroughMatches(MuxVideoUpload upload, JsonNode data) {
    String passthrough = optionalText(data, "passthrough");
    return passthrough == null || upload.getLesson().getId().toString().equals(passthrough);
  }

  private boolean isLatest(MuxVideoUpload upload) {
    return uploadRepository
        .findFirstByLessonIdOrderByCreatedAtDesc(upload.getLesson().getId())
        .map(latest -> latest.getId().equals(upload.getId()))
        .orElse(false);
  }

  private void markFailed(MuxVideoUpload upload, JsonNode data) {
    upload.setStatus(MuxUploadStatus.FAILED);
    String error = data.path("errors").isMissingNode() ? null : data.path("errors").toString();
    if (error != null && error.length() > MAX_ERROR_LENGTH) {
      error = error.substring(0, MAX_ERROR_LENGTH);
    }
    upload.setErrorMessage(error);
  }

  private String signedPlaybackId(JsonNode data) {
    for (JsonNode playback : data.path("playback_ids")) {
      if ("signed".equalsIgnoreCase(optionalText(playback, "policy"))) {
        return optionalText(playback, "id");
      }
    }
    return null;
  }

  private Integer durationSeconds(JsonNode data) {
    JsonNode duration = data.path("duration");
    if (!duration.isNumber() || duration.doubleValue() < 0) {
      return null;
    }
    return (int) Math.min(Integer.MAX_VALUE, Math.round(duration.doubleValue()));
  }

  private String firstText(JsonNode node, String... fields) {
    for (String field : fields) {
      String value = optionalText(node, field);
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private JsonNode parse(String rawBody) {
    try {
      JsonNode root = objectMapper.readTree(rawBody);
      if (root == null || !root.isObject()) {
        throw badRequest("Invalid Mux webhook payload");
      }
      return root;
    } catch (JsonProcessingException exception) {
      throw badRequest("Invalid Mux webhook payload");
    }
  }

  private String requiredText(JsonNode node, String field) {
    String value = optionalText(node, field);
    if (value == null) {
      throw badRequest("Invalid Mux webhook payload");
    }
    return value;
  }

  private String optionalText(JsonNode node, String field) {
    JsonNode value = node.path(field);
    if (!value.isTextual() || value.textValue().isBlank()) {
      return null;
    }
    return value.textValue();
  }

  private ResponseStatusException badRequest(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }
}

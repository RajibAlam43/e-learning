package com.gii.api.service.media;

import com.gii.api.exception.BadRequestApiException;
import com.gii.api.exception.ConflictApiException;
import com.gii.api.model.request.admin.CreateMediaAssetRequest;
import com.gii.api.model.request.admin.UpdateMediaAssetRequest;
import com.gii.api.model.response.MediaAssetResponse;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.course.MediaAsset;
import com.gii.common.enums.MediaStatus;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.course.MediaAssetRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class MediaAssetService {

  private final MediaAssetRepository mediaAssetRepository;
  private final LessonRepository lessonRepository;
  private final MediaAssetMapper mediaAssetMapper;

  // private final CurrentUserService currentUserService;

  public MediaAssetResponse createMediaAsset(
      UUID courseId, UUID lessonId, CreateMediaAssetRequest request) {
    Lesson lesson =
        lessonRepository
            .findById(lessonId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));

    if (!lesson.getCourse().getId().equals(courseId)) {
      throw new BadRequestApiException("Lesson does not belong to course");
    }

    if (mediaAssetRepository.existsByLessonId(lessonId)) {
      throw new ConflictApiException("Lesson already has a media asset");
    }

    validateCreateRequest(request);

    MediaAsset asset = mediaAssetMapper.toEntity(request, lesson);
    MediaAsset saved = mediaAssetRepository.save(asset);

    return mediaAssetMapper.toResponse(saved);
  }

  @Transactional(readOnly = true)
  public MediaAssetResponse getMediaAsset(UUID assetId) {
    MediaAsset asset = getAssetOrThrow(assetId);
    return mediaAssetMapper.toResponse(asset);
  }

  @Transactional
  public MediaAssetResponse updateMediaAsset(UUID assetId, UpdateMediaAssetRequest request) {
    MediaAsset asset = getAssetOrThrow(assetId);

    mediaAssetMapper.updateEntity(asset, request);
    validateAsset(asset);

    MediaAsset saved = mediaAssetRepository.save(asset);
    return mediaAssetMapper.toResponse(saved);
  }

  @Transactional
  public void deleteMediaAsset(UUID assetId) {
    MediaAsset asset = getAssetOrThrow(assetId);

    asset.setStatus(MediaStatus.DELETED);
    mediaAssetRepository.save(asset);
  }

  private MediaAsset getAssetOrThrow(UUID assetId) {
    return mediaAssetRepository
        .findById(assetId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media asset not found"));
  }

  private void validateCreateRequest(CreateMediaAssetRequest request) {
    if (request.provider() == null) {
      throw new BadRequestApiException("Provider is required");
    }

    if (request.title() == null || request.title().isBlank()) {
      throw new BadRequestApiException("Title is required");
    }
  }

  private void validateAsset(MediaAsset asset) {
    if (asset.getProvider() == null) {
      throw new BadRequestApiException("Media provider is required");
    }

    if (asset.getTitle() == null || asset.getTitle().isBlank()) {
      throw new BadRequestApiException("Media title is required");
    }

    switch (asset.getProvider()) {
      case YOUTUBE -> {
        require(asset.getProviderAssetId(), "YouTube video ID is required");
      }

      case MUX -> {
        require(asset.getPlaybackId(), "Mux playback ID is required");
      }

      case BUNNY -> {
        require(asset.getProviderAssetId(), "Bunny video ID is required");
        require(asset.getProviderLibraryId(), "Bunny library ID is required");
      }
      default -> {
        // No provider-specific required fields.
      }
    }
  }

  private void require(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new BadRequestApiException(message);
    }
  }
}

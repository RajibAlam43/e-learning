package com.gii.api.service.admin;

import com.gii.api.model.request.admin.CreateMediaAssetRequest;
import com.gii.api.model.request.admin.UpdateMediaAssetRequest;
import com.gii.api.model.response.admin.AdminMediaAssetResponse;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.course.MediaAsset;
import com.gii.common.enums.MediaAssetType;
import com.gii.common.enums.MediaStatus;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.course.MediaAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminMediaAssetManagementService {

  private final MediaAssetRepository mediaAssetRepository;
  private final LessonRepository lessonRepository;

  public AdminMediaAssetResponse create(CreateMediaAssetRequest request) {
    Lesson lesson =
        lessonRepository
            .findById(request.lessonId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
    if (mediaAssetRepository.existsByLessonId(lesson.getId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lesson already has media asset");
    }
    MediaAsset asset =
        MediaAsset.builder()
            .lesson(lesson)
            .provider(request.provider())
            .assetType(request.assetType() != null ? request.assetType() : MediaAssetType.VIDEO)
            .providerAssetId(request.providerAssetId())
            .providerLibraryId(request.providerLibraryId())
            .playbackId(request.playbackId())
            .playbackPolicy(request.playbackPolicy())
            .fileUrl(request.fileUrl())
            .title(request.title())
            .maxResolution(request.maxResolution())
            .durationSec(request.durationSec())
            .status(MediaStatus.READY)
            .build();
    return toResponse(mediaAssetRepository.save(asset));
  }

  public AdminMediaAssetResponse update(
      java.util.UUID mediaAssetId, UpdateMediaAssetRequest request) {
    MediaAsset asset =
        mediaAssetRepository
            .findById(mediaAssetId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media asset not found"));
    if (request.title() != null) {
      asset.setTitle(request.title());
    }
    if (request.provider() != null) {
      asset.setProvider(request.provider());
    }
    if (request.assetType() != null) {
      asset.setAssetType(request.assetType());
    }
    if (request.providerAssetId() != null) {
      asset.setProviderAssetId(request.providerAssetId());
    }
    if (request.providerLibraryId() != null) {
      asset.setProviderLibraryId(request.providerLibraryId());
    }
    if (request.playbackId() != null) {
      asset.setPlaybackId(request.playbackId());
    }
    if (request.playbackPolicy() != null) {
      asset.setPlaybackPolicy(request.playbackPolicy());
    }
    if (request.fileUrl() != null) {
      asset.setFileUrl(request.fileUrl());
    }
    if (request.maxResolution() != null) {
      asset.setMaxResolution(request.maxResolution());
    }
    if (request.durationSec() != null) {
      asset.setDurationSec(request.durationSec());
    }
    if (request.status() != null) {
      asset.setStatus(request.status());
    }
    if (request.preferredPlaybackMode() != null) {
      asset.setPreferredPlaybackMode(
          com.gii.common.enums.PlaybackMode.valueOf(request.preferredPlaybackMode().toUpperCase()));
    }
    return toResponse(mediaAssetRepository.save(asset));
  }

  private AdminMediaAssetResponse toResponse(MediaAsset asset) {
    return AdminMediaAssetResponse.builder()
        .mediaAssetId(asset.getId())
        .lessonId(asset.getLesson().getId())
        .provider(asset.getProvider().name())
        .assetType(asset.getAssetType().name())
        .providerAssetId(asset.getProviderAssetId())
        .providerLibraryId(asset.getProviderLibraryId())
        .playbackId(asset.getPlaybackId())
        .playbackPolicy(asset.getPlaybackPolicy() != null ? asset.getPlaybackPolicy().name() : null)
        .fileUrl(asset.getFileUrl())
        .title(asset.getTitle())
        .maxResolution(asset.getMaxResolution())
        .durationSec(asset.getDurationSec())
        .status(asset.getStatus().name())
        .preferredPlaybackMode(
            asset.getPreferredPlaybackMode() != null
                ? asset.getPreferredPlaybackMode().name()
                : null)
        .createdBy(asset.getCreatedBy() != null ? asset.getCreatedBy().getId() : null)
        .createdAt(asset.getCreatedAt())
        .updatedAt(asset.getUpdatedAt())
        .build();
  }
}

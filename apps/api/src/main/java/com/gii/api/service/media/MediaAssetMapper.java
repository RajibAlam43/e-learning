package com.gii.api.service.media;

import com.gii.api.model.request.admin.CreateMediaAssetRequest;
import com.gii.api.model.request.admin.UpdateMediaAssetRequest;
import com.gii.api.model.response.MediaAssetResponse;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.course.MediaAsset;
import com.gii.common.enums.MediaAssetType;
import com.gii.common.enums.MediaStatus;
import org.springframework.stereotype.Component;

@Component
public class MediaAssetMapper {

    public MediaAsset toEntity(CreateMediaAssetRequest request, Lesson lesson) {
        return MediaAsset.builder()
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
    }

    public void updateEntity(MediaAsset asset, UpdateMediaAssetRequest request) {
        if (request.provider() != null) {
            asset.setProvider(request.provider());
        }

        if (request.assetType() != null) {
            asset.setAssetType(request.assetType());
        }

        if (request.status() != null) {
            asset.setStatus(request.status());
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

        if (request.title() != null) {
            asset.setTitle(request.title());
        }

        if (request.maxResolution() != null) {
            asset.setMaxResolution(request.maxResolution());
        }

        if (request.durationSec() != null) {
            asset.setDurationSec(request.durationSec());
        }
    }

    public MediaAssetResponse toResponse(MediaAsset asset) {
        return new MediaAssetResponse(
                asset.getId(),
                asset.getLesson().getId(),
                asset.getProvider(),
                asset.getAssetType(),
                asset.getProviderAssetId(),
                asset.getProviderLibraryId(),
                asset.getPlaybackId(),
                asset.getPlaybackPolicy(),
                asset.getFileUrl(),
                asset.getTitle(),
                asset.getMaxResolution(),
                asset.getDurationSec(),
                asset.getStatus(),
                asset.getCreatedBy() != null ? asset.getCreatedBy().getId() : null,
                asset.getCreatedAt(),
                asset.getUpdatedAt()
        );
    }
}

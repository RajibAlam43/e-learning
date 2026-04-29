package com.gii.api.model.response.admin;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record AdminMediaAssetResponse(
        UUID mediaAssetId,
        UUID lessonId,
        String provider,
        String assetType,
        String providerAssetId,
        String providerLibraryId,
        String playbackId,
        String playbackPolicy,
        String fileUrl,
        String title,
        String maxResolution,
        Integer durationSec,
        String status,  // UPLOADING, PROCESSING, READY, FAILED, DELETED
        String preferredPlaybackMode,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt
) {}
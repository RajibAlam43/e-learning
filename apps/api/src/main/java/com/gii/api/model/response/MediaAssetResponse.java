package com.gii.api.model.response;

import com.gii.common.enums.MediaAssetType;
import com.gii.common.enums.MediaProvider;
import com.gii.common.enums.MediaStatus;
import com.gii.common.enums.PlaybackPolicy;
import java.time.Instant;
import java.util.UUID;

public record MediaAssetResponse(
    UUID id,
    UUID lessonId,
    MediaProvider provider,
    MediaAssetType assetType,
    String providerAssetId,
    String providerLibraryId,
    String playbackId,
    PlaybackPolicy playbackPolicy,
    String fileUrl,
    String title,
    String maxResolution,
    Integer durationSec,
    MediaStatus status,
    UUID createdByUserId,
    Instant createdAt,
    Instant updatedAt) {}

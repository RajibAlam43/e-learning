package com.gii.api.model.request.admin;

import com.gii.common.enums.MediaAssetType;
import com.gii.common.enums.MediaProvider;
import com.gii.common.enums.PlaybackPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record CreateMediaAssetRequest(
        @NotNull UUID lessonId,
        @NotBlank MediaProvider provider,  // MUX, YOUTUBE, BUNNY
        @NotBlank MediaAssetType assetType,  // VIDEO, PDF, IMAGE
        String providerAssetId,
        String providerLibraryId,
        String playbackId,
        PlaybackPolicy playbackPolicy,  // PUBLIC, SIGNED
        String fileUrl,
        @NotBlank String title,
        String maxResolution,
        Integer durationSec,
        String preferredPlaybackMode  // IFRAME, HLS
) {}
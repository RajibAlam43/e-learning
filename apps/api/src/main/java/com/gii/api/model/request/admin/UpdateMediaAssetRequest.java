package com.gii.api.model.request.admin;

import com.gii.common.enums.MediaAssetType;
import com.gii.common.enums.MediaProvider;
import com.gii.common.enums.MediaStatus;
import com.gii.common.enums.PlaybackPolicy;
import lombok.Builder;

@Builder
public record UpdateMediaAssetRequest(
        String title,  // Optional: update title
        String providerAssetId,  // Optional: update provider asset ID
        String providerLibraryId,  // Optional: update provider library ID
        String playbackId,  // Optional: update Mux playback ID
        PlaybackPolicy playbackPolicy,  // Optional: PUBLIC, SIGNED
        String fileUrl,  // Optional: update file URL (for PDF/IMAGE)
        String maxResolution,  // Optional: update resolution
        Integer durationSec,  // Optional: update duration
        String preferredPlaybackMode,  // Optional: IFRAME, HLS
        MediaStatus status,  // Optional: update status (READY, FAILED, etc.)
        MediaProvider provider,
        MediaAssetType assetType
) {}
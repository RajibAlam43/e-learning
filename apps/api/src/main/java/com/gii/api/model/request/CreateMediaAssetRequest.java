package com.gii.api.model.request;

import com.gii.common.enums.MediaAssetType;
import com.gii.common.enums.MediaProvider;
import com.gii.common.enums.PlaybackPolicy;

public record CreateMediaAssetRequest(
        MediaProvider provider,
        MediaAssetType assetType,
        String providerAssetId,
        String providerLibraryId,
        String playbackId,
        PlaybackPolicy playbackPolicy,
        String fileUrl,
        String title,
        String maxResolution,
        Integer durationSec
) {}

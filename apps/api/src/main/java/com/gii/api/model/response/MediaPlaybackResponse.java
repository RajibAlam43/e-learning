package com.gii.api.model.response;

import com.gii.common.enums.PlaybackMode;
import com.gii.common.enums.MediaProvider;

import java.time.Instant;

public record MediaPlaybackResponse(
        MediaProvider provider,
        PlaybackMode playbackMode,
        String embedUrl,
        String hlsUrl,
        String playbackUrl,
        String playbackId,
        String token,
        Instant expiresAt
) {}

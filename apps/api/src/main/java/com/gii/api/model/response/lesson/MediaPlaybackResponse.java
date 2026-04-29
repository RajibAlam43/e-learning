package com.gii.api.model.response.lesson;

import com.gii.common.enums.MediaAssetType;
import com.gii.common.enums.MediaProvider;
import com.gii.common.enums.PlaybackMode;
import lombok.Builder;

import java.util.UUID;

@Builder
public record MediaPlaybackResponse(
        UUID mediaAssetId,
        MediaAssetType assetType,  // VIDEO, PDF, IMAGE
        MediaProvider provider,  // MUX, YOUTUBE, BUNNY
        String title,
        Integer durationSec,  // For video
        String maxResolution,  // For video, e.g., "1080p"

        // Playback URLs/IDs - populate based on provider
        String youtubeVideoId,  // For YouTube
        String muxPlaybackId,  // For Mux
        String bunnyAssetId,  // For Bunny
        String fileUrl,  // For PDF/IMAGE

        // Playback configuration
        PlaybackMode preferredPlaybackMode,  // IFRAME or HLS
        Boolean requiresSignedUrl,  // Whether URL needs signing

        // Player hints
        String subtitlesUrl,  // Optional: captions/subtitle URL
        String thumbnailUrl  // Optional: cover image
) {}
package com.gii.api.model.response;

import com.gii.common.enums.MediaProvider;
import lombok.Builder;

import java.util.UUID;

@Builder
public record LessonPlaybackResponse(
        UUID lessonId,
        MediaProvider provider,
        String sourceId,
        String playbackId,
        String muxToken
) {}

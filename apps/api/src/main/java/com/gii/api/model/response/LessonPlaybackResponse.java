package com.gii.api.model.response;

import com.gii.common.enums.MediaProvider;
import java.util.UUID;
import lombok.Builder;

@Builder
public record LessonPlaybackResponse(
    UUID lessonId, MediaProvider provider, String sourceId, String playbackId, String muxToken) {}

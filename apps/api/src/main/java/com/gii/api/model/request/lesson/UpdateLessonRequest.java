package com.gii.api.model.request.lesson;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record UpdateLessonRequest(
        String title,
        String slug,
        Integer position,
        String lessonType,
        Boolean isMandatory,
        Boolean isFree,
        Integer durationSeconds,
        String thumbnailUrl,
        String transcriptUrl,
        String releaseType,
        Instant releaseAt,
        Integer unlockAfterDays,
        UUID mediaAssetId
) {}
package com.gii.api.model.response.lesson;

import com.gii.common.enums.LessonType;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record LessonProgressSummaryResponse(
    UUID lessonId,
    String lessonTitle,
    Integer position,
    LessonType lessonType,

    // Progress state
    Boolean completed,
    Instant completedAt,
    Integer lastPositionSec, // For videos

    // Accessibility
    Boolean isAccessible) {}

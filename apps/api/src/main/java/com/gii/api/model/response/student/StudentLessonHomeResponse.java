package com.gii.api.model.response.student;

import com.gii.common.enums.LessonType;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record StudentLessonHomeResponse(
    UUID lessonId,
    String lessonTitle,
    Integer position,
    LessonType lessonType, // VIDEO, QUIZ, LIVE, ASSIGNMENT, PDF

    // Progress state (for student)
    Boolean completed,
    Instant completedAt,
    Integer lastPositionSec, // For video lessons

    // Accessibility
    Boolean isAccessible,
    String durationLabel, // e.g., "45 minutes", "15 questions"
    Boolean isFree, // Purchasable separately

    // Navigation
    String nextLessonId,
    String previousLessonId) {}

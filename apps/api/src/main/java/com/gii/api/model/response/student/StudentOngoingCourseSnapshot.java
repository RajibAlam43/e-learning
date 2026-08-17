package com.gii.api.model.response.student;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record StudentOngoingCourseSnapshot(
    UUID courseId,
    String courseName,
    String courseSlug,
    String courseThumbnailUrl,

    // Progress
    Double completionPercentage,
    Integer completedLessons,
    Integer totalLessons,
    Integer completedItems,
    Integer totalItems,

    // Enrollment info
    Instant enrolledAt,
    Instant expiresAt,
    Boolean isExpiring, // Flag if expiring soon

    // Quick action
    String nextLessonId // For "Continue Learning" button
    ) {}

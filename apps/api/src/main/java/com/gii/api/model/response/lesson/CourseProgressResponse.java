package com.gii.api.model.response.lesson;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record CourseProgressResponse(
        UUID courseId,
        String courseName,
        String courseSlug,

        // Completion metrics
        Integer totalLessons,
        Integer completedLessons,
        Integer pendingLessons,
        Double completionPercentage,  // 0-100

        // Enrollment info
        Instant enrolledAt,
        Instant completedAt,  // Null if not yet completed
        Instant expiresAt,  // Null if no expiration

        // Section-level progress
        List<SectionProgressResponse> sections
) {}
package com.gii.api.model.response.student;

import com.gii.common.enums.EnrollmentStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record StudentCourseSummaryResponse(
    UUID courseId,
    String courseName,
    String courseSlug,
    String instructorName,
    String courseThumbnailUrl,

    // Progress
    Double completionPercentage,
    Integer completedLessons,
    Integer totalLessons,

    // Enrollment details
    EnrollmentStatus enrollmentStatus, // ACTIVE, REFUNDED, REVOKED
    Instant enrolledAt,
    Instant completedAt, // Null if not completed
    Instant expiresAt, // Null if no expiration

    // Course info
    String courseLevel, // BEGINNER, INTERMEDIATE, ADVANCED
    String language, // BN, EN

    // Certificates
    Boolean hasCertificate,
    String certificateCode // If earned
    ) {}

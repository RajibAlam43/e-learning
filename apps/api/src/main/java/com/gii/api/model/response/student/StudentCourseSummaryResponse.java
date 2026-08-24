package com.gii.api.model.response.student;

import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.StudyMode;
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
    Integer completedItems,
    Integer totalItems,

    // Enrollment details
    EnrollmentStatus enrollmentStatus, // ACTIVE, REFUNDED, REVOKED
    Instant enrolledAt,
    Instant completedAt, // Null if not completed
    Instant expiresAt, // Null if no expiration

    // Course info
    String courseLevel, // BEGINNER, INTERMEDIATE, ADVANCED
    String language, // BN, EN
    StudyMode studyMode,
    String timezone,
    Instant startsAt,
    Instant endsAt,

    // Certificates
    Boolean hasCertificate,
    String certificateCode // If earned
    ) {}

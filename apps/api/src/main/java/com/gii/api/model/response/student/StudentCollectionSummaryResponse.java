package com.gii.api.model.response.student;

import com.gii.common.enums.CollectionType;
import com.gii.common.enums.EnrollmentStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record StudentCollectionSummaryResponse(
    UUID collectionId,
    String collectionName,
    String collectionSlug,
    CollectionType collectionType,
    String thumbnailObjectKey,
    Double progressPercentage,
    Integer completedLessons,
    Integer totalLessons,
    Integer courseCount,
    EnrollmentStatus enrollmentStatus,
    Instant enrolledAt,
    Instant completedAt,
    Instant expiresAt) {}

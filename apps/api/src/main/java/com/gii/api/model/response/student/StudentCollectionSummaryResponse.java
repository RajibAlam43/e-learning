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
    String thumbnailUrl,
    Double progressPercentage,
    Integer completedLessons,
    Integer totalLessons,
    Integer completedItems,
    Integer totalItems,
    Integer courseCount,
    EnrollmentStatus enrollmentStatus,
    Instant enrolledAt,
    Instant completedAt,
    Instant expiresAt) {}

package com.gii.api.model.response.student;

import com.gii.common.enums.CollectionType;
import com.gii.common.enums.EnrollmentStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record StudentCollectionDetailsResponse(
    UUID collectionId,
    String collectionName,
    String collectionSlug,
    CollectionType collectionType,
    String thumbnailUrl,
    String shortDescription,
    String description,
    Double progressPercentage,
    Integer completedLessons,
    Integer totalLessons,
    Integer courseCount,
    EnrollmentStatus enrollmentStatus,
    Instant enrolledAt,
    Instant completedAt,
    Instant expiresAt,
    List<StudentCollectionCourseProgressResponse> courses) {}

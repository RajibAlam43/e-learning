package com.gii.api.model.response.admin;

import com.gii.common.enums.ReviewStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminCourseReviewResponse(
    UUID reviewId,
    UUID courseId,
    String courseTitle,
    UUID studentId,
    String studentName,
    Integer rating,
    String reviewText,
    ReviewStatus status,
    Instant createdAt,
    Instant updatedAt) {}

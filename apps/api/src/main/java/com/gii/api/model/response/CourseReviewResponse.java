package com.gii.api.model.response;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CourseReviewResponse(
    UUID reviewId,
    UUID courseId,
    String studentName,
    Integer rating,
    String reviewText,
    Instant createdAt) {}

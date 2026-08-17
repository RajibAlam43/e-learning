package com.gii.api.model.response.admin;

import com.gii.common.enums.PublishStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminCourseSummaryResponse(
    UUID courseId,
    String title,
    String slug,
    String thumbnailUrl,
    PublishStatus status,
    BigDecimal priceBdt,
    Boolean isFree,
    String instructorName,
    Integer totalEnrolled,
    Boolean isFeatured,
    Integer featuredPosition,
    Instant featuredAt,
    Instant publishedAt,
    Instant createdAt) {}

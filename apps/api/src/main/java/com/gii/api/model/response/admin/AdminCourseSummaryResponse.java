package com.gii.api.model.response.admin;

import com.gii.common.enums.PublishStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record AdminCourseSummaryResponse(
        UUID courseId,
        String title,
        String slug,
        PublishStatus status,
        BigDecimal priceBdt,
        Boolean isFree,
        String instructorName,
        Integer totalEnrolled,
        Instant publishedAt,
        Instant createdAt
) {}
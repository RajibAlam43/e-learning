package com.gii.api.model.response;

import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record CourseSummaryResponse(
        UUID id,
        String title,
        String slug,
        String shortDescription,
        CourseLanguage language,
        CourseLevel level,
        String categoryName,
        String thumbnailUrl,
        BigDecimal priceBdt,
        Instant publishedAt,
        String instructorName,
        int page
) {}

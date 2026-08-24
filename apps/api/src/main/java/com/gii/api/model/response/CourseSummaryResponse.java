package com.gii.api.model.response;

import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.StudyMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CourseSummaryResponse(
    UUID id,
    String title,
    String slug,
    String shortDescription,
    CourseLanguage language,
    CourseLevel level,
    List<String> categoryNames,
    String thumbnailUrl,
    BigDecimal priceBdt,
    StudyMode studyMode,
    Instant startsAt,
    Instant endsAt,
    Instant publishedAt,
    List<String> instructorNames,
    Double averageRating,
    Long totalReviews) {}

package com.gii.api.model.response;

import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record CourseDetailsResponse(
        UUID id,
        String title,
        String slug,
        String shortDescription,
        String description,
        CourseLanguage language,
        CourseLevel level,
        String thumbnailUrl,
        Integer priceBdt,
        Instant publishedAt,
        CategoryResponse category,
        InstructorSummaryResponse instructor,
        List<CourseSectionResponse> sections
) {}

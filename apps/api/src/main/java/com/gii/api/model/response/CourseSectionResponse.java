package com.gii.api.model.response;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record CourseSectionResponse(
        UUID id,
        String title,
        String description,
        Integer position,
        List<LessonSummaryResponse> lessons
) {}

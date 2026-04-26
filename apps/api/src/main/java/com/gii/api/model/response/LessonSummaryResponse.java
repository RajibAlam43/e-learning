package com.gii.api.model.response;

import com.gii.common.enums.LessonType;
import lombok.Builder;

import java.util.UUID;

@Builder
public record LessonSummaryResponse(
        UUID id,
        String title,
        String slug,
        Integer position,
        LessonType lessonType,
        Boolean isPreviewFree,
        Integer durationSeconds
) {}

package com.gii.api.model.request.admin;

import lombok.Builder;

import java.util.UUID;

@Builder
public record LessonReorderRequest(
        UUID lessonId,
        Integer newPosition
) {}

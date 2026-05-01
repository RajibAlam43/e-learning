package com.gii.api.model.request.admin;

import java.util.UUID;
import lombok.Builder;

@Builder
public record LessonReorderRequest(UUID lessonId, Integer newPosition) {}

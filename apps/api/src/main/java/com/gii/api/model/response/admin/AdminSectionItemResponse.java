package com.gii.api.model.response.admin;

import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminSectionItemResponse(
    UUID itemId,
    String itemType,
    Integer position,
    AdminLessonSummaryResponse lesson,
    AdminQuizSummaryResponse quiz) {}


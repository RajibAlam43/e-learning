package com.gii.api.model.response.admin;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminLessonSummaryResponse(
    UUID lessonId,
    String title,
    String slug,
    Integer position,
    String lessonType,
    String status, // DRAFT, PUBLISHED, ARCHIVED
    Boolean isMandatory,
    Boolean isFree,
    Integer durationSeconds,
    Instant createdAt) {}

package com.gii.api.model.response.admin;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminLessonDetailResponse(
    UUID lessonId,
    String title,
    String slug,
    Integer position,
    String lessonType,
    String status,
    Boolean isMandatory,
    Boolean isFree,
    Integer durationSeconds,
    String thumbnailUrl,
    String transcriptUrl,
    String releaseType,
    Instant releaseAt,
    Integer unlockAfterDays,
    Instant publishedAt,
    Instant createdAt,
    Instant updatedAt,
    AdminMediaAssetResponse mediaAsset,
    List<AdminLessonResourceResponse> resources) {}

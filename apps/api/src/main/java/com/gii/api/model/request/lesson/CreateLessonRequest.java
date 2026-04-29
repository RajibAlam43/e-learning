package com.gii.api.model.request.lesson;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record CreateLessonRequest(
        @NotBlank String title,
        @NotBlank String slug,
        @NotNull Integer position,
        @NotBlank String lessonType,  // VIDEO, QUIZ, LIVE, ASSIGNMENT, PDF
        Boolean isMandatory,
        Boolean isFree,
        Integer durationSeconds,
        String thumbnailUrl,
        String transcriptUrl,
        String releaseType,
        Instant releaseAt,
        Integer unlockAfterDays,
        UUID mediaAssetId  // Optional: link to existing media
) {}
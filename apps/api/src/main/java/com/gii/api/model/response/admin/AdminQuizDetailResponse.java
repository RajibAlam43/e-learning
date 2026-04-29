package com.gii.api.model.response.admin;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record AdminQuizDetailResponse(
        UUID quizId,
        String title,
        Integer passingScorePct,
        Integer maxAttempts,
        Integer timeLimitSec,
        String status,  // DRAFT, PUBLISHED, ARCHIVED
        Instant createdAt,
        Instant updatedAt,
        List<AdminQuizQuestionResponse> questions
) {}
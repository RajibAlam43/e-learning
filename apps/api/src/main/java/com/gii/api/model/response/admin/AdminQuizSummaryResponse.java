package com.gii.api.model.response.admin;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminQuizSummaryResponse(
    UUID quizId,
    UUID sectionId,
    Integer position,
    String title,
    String status,
    Integer passingScorePct,
    Integer maxAttempts,
    Integer timeLimitSec,
    Instant createdAt) {}


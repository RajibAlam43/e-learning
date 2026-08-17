package com.gii.api.model.response.student;

import java.util.UUID;
import lombok.Builder;

@Builder
public record StudentQuizHomeResponse(
    UUID quizId,
    String quizTitle,
    Integer position,
    Boolean isAccessible,
    String accessReason,
    Integer passingScorePct,
    Integer maxAttempts,
    Integer timeLimitSec,
    Boolean completed) {}

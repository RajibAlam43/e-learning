package com.gii.api.model.response.quiz;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record QuizAttemptStartResponse(
        UUID attemptId,
        Integer attemptNumber,
        
        // Timing info
        Instant startedAt,
        Instant deadline,  // When attempt expires (if time limit)
        Long timeRemainingSeconds,  // Seconds available
        
        // Quiz configuration (for timer, etc.)
        Integer timeLimitSec,
        Integer totalQuestions,
        Integer totalPoints,
        
        // Instructions/meta
        String quizTitle,
        String instructions  // Re-confirm quiz-level instructions
) {}
package com.gii.api.model.response.quiz;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record QuizAttemptSummaryResponse(
    UUID attemptId,
    Integer attemptNumber,

    // Scoring
    Integer scorePct,
    Boolean passed, // Passed or failed
    Integer totalPoints,
    Integer earnedPoints,

    // Timing
    Instant startedAt,
    Instant submittedAt, // Null if still in progress
    Long durationSeconds, // Null if not submitted

    // Status
    String status, // "IN_PROGRESS", "SUBMITTED", "GRADED"

    // Quick action
    String resultUrl // Link to detailed result view
) {}

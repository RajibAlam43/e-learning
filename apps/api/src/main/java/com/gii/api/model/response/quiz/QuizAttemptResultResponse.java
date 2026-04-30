package com.gii.api.model.response.quiz;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record QuizAttemptResultResponse(
    // Attempt metadata
    UUID attemptId,
    UUID quizId,
    String quizTitle,
    Integer attemptNumber,

    // Scoring
    Integer scorePct,
    Integer totalPoints,
    Integer earnedPoints,
    Integer passingScorePct,
    Boolean passed, // scorePct >= passingScorePct

    // Timing
    Instant startedAt,
    Instant submittedAt,
    Long durationSeconds, // How long attempt took

    // Question-by-question breakdown
    List<QuizAttemptQuestionResultResponse> questionResults,

    // User's progress on quiz
    Integer totalAttempts,
    Boolean canRetry, // Based on maxAttempts
    Integer bestScorePct, // Best across all attempts

    // Feedback
    String feedbackMessage, // e.g., "Congratulations! You passed the quiz."
    String nextAction // e.g., "COMPLETE_LESSON", "RETRY_QUIZ", "CONTINUE"
) {}

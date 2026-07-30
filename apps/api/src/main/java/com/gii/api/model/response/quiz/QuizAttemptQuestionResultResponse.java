package com.gii.api.model.response.quiz;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record QuizAttemptQuestionResultResponse(
    UUID questionId,
    Integer position,
    String questionText,
    Integer points,

    // User's answer
    UUID userChoiceId,
    String userChoiceText,
    Boolean userAnswerCorrect,

    // Correct answer (shown after submission)
    UUID correctChoiceId,
    String correctChoiceText,

    // Full choices for reference
    List<QuizResultChoiceResponse> allChoices,

    // Explanation (if provided by instructor)
    String explanation,

    // Points earned for this question
    Integer earnedPoints, // 0 or points based on correctness

    // Feedback
    String feedbackMessage // e.g., "Correct!", "Incorrect. The correct answer is..."
) {}

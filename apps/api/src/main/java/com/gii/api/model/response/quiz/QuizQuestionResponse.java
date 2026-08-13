package com.gii.api.model.response.quiz;

import com.gii.common.enums.QuestionType;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record QuizQuestionResponse(
    UUID questionId,
    Integer position, // Question order
    String questionText,
    QuestionType questionType, // MCQ (extensible for future types)
    Integer points, // Points for correct answer

    // Choices for MCQ
    List<QuizChoiceResponse> choices,

    // UI hints (shown only during attempt, not in preview)
    Boolean showAsShuffled // Whether to shuffle choices
    ) {}

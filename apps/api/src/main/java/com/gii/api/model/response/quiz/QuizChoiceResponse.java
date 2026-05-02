package com.gii.api.model.response.quiz;

import java.util.UUID;
import lombok.Builder;

@Builder
public record QuizChoiceResponse(
    UUID choiceId,
    String choiceText,
    // Note: isCorrect is NOT exposed here during the attempt
    // It will only be shown in results after submission
    Boolean isCorrect // Null or false during attempt, true after submission if correct
) {}

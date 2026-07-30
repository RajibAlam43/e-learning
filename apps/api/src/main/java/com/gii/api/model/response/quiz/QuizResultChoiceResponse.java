package com.gii.api.model.response.quiz;

import java.util.UUID;
import lombok.Builder;

@Builder
public record QuizResultChoiceResponse(
    UUID choiceId,
    String choiceText,
    Boolean isCorrect, // Now visible after submission
    Boolean wasUserChoice // Whether user selected this
) {}

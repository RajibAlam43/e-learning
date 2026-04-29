package com.gii.api.model.response.quiz;

import lombok.Builder;

import java.util.UUID;

@Builder
public record QuizResultChoiceResponse(
        UUID choiceId,
        String choiceText,
        Boolean isCorrect,  // Now visible after submission
        Boolean wasUserChoice  // Whether user selected this
) {}
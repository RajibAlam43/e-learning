package com.gii.api.model.response.admin;

import lombok.Builder;

import java.util.UUID;

@Builder
public record AdminQuizChoiceResponse(
        UUID choiceId,
        String choiceText,
        Boolean isCorrect
) {}
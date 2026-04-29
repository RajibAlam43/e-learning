package com.gii.api.model.request.admin;

import lombok.Builder;

import java.util.UUID;

@Builder
public record UpdateQuizChoiceRequest(
        UUID choiceId,
        String choiceText,
        Boolean isCorrect
) {}

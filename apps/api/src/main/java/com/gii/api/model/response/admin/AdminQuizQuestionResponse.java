package com.gii.api.model.response.admin;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record AdminQuizQuestionResponse(
        UUID questionId,
        Integer position,
        String questionText,
        String questionType,
        Integer points,
        String explanationText,
        List<AdminQuizChoiceResponse> choices
) {}
package com.gii.api.model.request.admin;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record UpdateQuizQuestionRequest(
        UUID questionId,
        Integer position,
        String questionText,
        String questionType,
        Integer points,
        String explanationText,
        List<UpdateQuizChoiceRequest> choices
) {}

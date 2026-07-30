package com.gii.api.model.response.admin;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminQuizQuestionResponse(
    UUID questionId,
    Integer position,
    String questionText,
    String questionTextEn,
    String questionType,
    Integer points,
    String explanationText,
    String explanationTextEn,
    List<AdminQuizChoiceResponse> choices) {}

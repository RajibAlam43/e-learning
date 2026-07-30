package com.gii.api.model.request.admin;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record UpdateQuizQuestionRequest(
    UUID questionId,
    Integer position,
    String questionText,
    String questionTextEn,
    String questionType,
    Integer points,
    String explanationText,
    String explanationTextEn,
    List<UpdateQuizChoiceRequest> choices) {}

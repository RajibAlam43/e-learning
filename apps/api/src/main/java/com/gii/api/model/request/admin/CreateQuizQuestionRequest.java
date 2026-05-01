package com.gii.api.model.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Builder;

@Builder
public record CreateQuizQuestionRequest(
    @NotNull Integer position,
    @NotBlank String questionText,
    @NotBlank String questionType, // MCQ
    @NotNull Integer points,
    String explanationText,
    @NotEmpty List<CreateQuizChoiceRequest> choices) {}

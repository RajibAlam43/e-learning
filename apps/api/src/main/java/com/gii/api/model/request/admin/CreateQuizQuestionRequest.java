package com.gii.api.model.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

@Builder
public record CreateQuizQuestionRequest(
        @NotNull Integer position,
        @NotBlank String questionText,
        @NotBlank String questionType,  // MCQ
        @NotNull Integer points,
        String explanationText,
        @NotEmpty List<CreateQuizChoiceRequest> choices
) {}

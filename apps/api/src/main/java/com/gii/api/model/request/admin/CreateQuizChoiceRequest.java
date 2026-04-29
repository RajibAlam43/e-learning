package com.gii.api.model.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CreateQuizChoiceRequest(
        @NotBlank String choiceText,
        @NotNull Boolean isCorrect
) {}

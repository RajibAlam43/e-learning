package com.gii.api.model.request.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

@Builder
public record CreateQuizRequest(
        @NotBlank String title,
        @NotNull Integer passingScorePct,
        @NotNull Integer maxAttempts,
        Integer timeLimitSec,
        @NotEmpty @Valid List<CreateQuizQuestionRequest> questions
) {}


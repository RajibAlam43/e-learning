package com.gii.api.model.request.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CreateQuizRequest(
    @NotNull UUID sectionId,
    @NotNull Integer position,
    @NotBlank String title,
    @NotNull Integer passingScorePct,
    @NotNull Integer maxAttempts,
    Integer timeLimitSec,
    @NotEmpty @Valid List<CreateQuizQuestionRequest> questions) {}

package com.gii.api.model.request.quiz;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Builder;

@Builder
public record QuizAnswerSubmissionRequest(
    @NotNull UUID questionId, // Which question this answer is for
    @NotNull UUID choiceId // Which choice was selected
    ) {}

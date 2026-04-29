package com.gii.api.model.request.quiz;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record QuizAnswerSubmissionRequest(
        @NotNull UUID questionId,  // Which question this answer is for
        @NotNull UUID choiceId  // Which choice was selected
) {}
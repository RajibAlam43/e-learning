package com.gii.api.model.request.quiz;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;

@Builder
public record SubmitQuizAttemptRequest(
        @NotEmpty(message = "At least one answer must be provided")
        @Valid
        List<QuizAnswerSubmissionRequest> answers
) {}
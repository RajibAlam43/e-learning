package com.gii.api.model.request.quiz;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Builder;

@Builder
public record SubmitQuizAttemptRequest(
    @NotEmpty(message = "At least one answer must be provided") @Valid
        List<QuizAnswerSubmissionRequest> answers) {}

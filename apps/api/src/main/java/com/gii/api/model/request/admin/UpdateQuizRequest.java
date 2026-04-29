package com.gii.api.model.request.admin;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record UpdateQuizRequest(
        String title,
        Integer passingScorePct,
        Integer maxAttempts,
        Integer timeLimitSec,
        List<UpdateQuizQuestionRequest> questions
) {}


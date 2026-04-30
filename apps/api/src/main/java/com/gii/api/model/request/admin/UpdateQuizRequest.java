package com.gii.api.model.request.admin;

import java.util.List;
import lombok.Builder;

@Builder
public record UpdateQuizRequest(
    String title,
    Integer passingScorePct,
    Integer maxAttempts,
    Integer timeLimitSec,
    List<UpdateQuizQuestionRequest> questions) {}

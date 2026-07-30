package com.gii.api.model.request.admin;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record UpdateQuizRequest(
    UUID sectionId,
    Integer position,
    String title,
    String titleEn,
    Integer passingScorePct,
    Integer maxAttempts,
    Integer timeLimitSec,
    List<UpdateQuizQuestionRequest> questions) {}

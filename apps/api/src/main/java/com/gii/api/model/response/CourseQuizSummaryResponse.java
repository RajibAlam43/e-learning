package com.gii.api.model.response;

import java.util.UUID;
import lombok.Builder;

@Builder
public record CourseQuizSummaryResponse(
    UUID id,
    String title,
    Long questionCount,
    Integer passingScorePct,
    Integer maxAttempts,
    Integer timeLimitSec) {}

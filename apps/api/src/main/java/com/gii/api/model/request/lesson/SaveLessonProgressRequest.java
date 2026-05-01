package com.gii.api.model.request.lesson;

import jakarta.validation.constraints.Min;
import lombok.Builder;

@Builder
public record SaveLessonProgressRequest(
    Boolean completed, // Optional: mark lesson as completed
    @Min(0) Integer lastPositionSec // Optional: current video playback position in seconds
) {}

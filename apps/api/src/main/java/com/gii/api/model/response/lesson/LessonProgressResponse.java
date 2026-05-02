package com.gii.api.model.response.lesson;

import java.time.Instant;
import lombok.Builder;

@Builder
public record LessonProgressResponse(
    Boolean completed, // Whether user completed this lesson
    Instant completedAt, // When user completed (null if not completed)
    Integer lastPositionSec, // Last video playback position in seconds
    Instant lastUpdatedAt // When progress was last updated
    ) {}

package com.gii.api.model.response.student;

import java.util.UUID;
import lombok.Builder;

@Builder
public record StudentCollectionCourseProgressResponse(
    UUID courseId,
    String courseName,
    String courseSlug,
    String courseThumbnailUrl,
    Double completionPercentage,
    Integer completedLessons,
    Integer totalLessons,
    Integer completedItems,
    Integer totalItems) {}

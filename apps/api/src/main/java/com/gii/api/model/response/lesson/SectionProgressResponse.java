package com.gii.api.model.response.lesson;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record SectionProgressResponse(
    UUID sectionId,
    String sectionTitle,
    Integer position,

    // Completion metrics for section
    Integer totalLessons,
    Integer completedLessons,
    Double completionPercentage, // 0-100

    // Lesson-level progress
    List<LessonProgressSummaryResponse> lessons) {}

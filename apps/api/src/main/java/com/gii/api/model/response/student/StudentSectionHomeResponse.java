package com.gii.api.model.response.student;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record StudentSectionHomeResponse(
        UUID sectionId,
        String sectionTitle,
        Integer position,
        String description,
        
        // Progress
        Double completionPercentage,
        Integer completedLessons,
        Integer totalLessons,
        
        // Access status
        Boolean isAccessible,  // Based on release type/drip content
        String accessReason,  // e.g., "AVAILABLE", "PENDING_RELEASE", "LOCKED"
        
        // Lessons in section
        List<StudentLessonHomeResponse> lessons
) {}
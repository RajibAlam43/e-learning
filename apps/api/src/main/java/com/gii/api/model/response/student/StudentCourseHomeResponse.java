package com.gii.api.model.response.student;

import com.gii.common.enums.EnrollmentStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record StudentCourseHomeResponse(
        // Course metadata
        UUID courseId,
        String courseName,
        String courseSlug,
        String description,
        String thumbnailUrl,
        String instructor,
        String courseLevel,
        
        // Enrollment status
        EnrollmentStatus enrollmentStatus,
        Instant enrolledAt,
        Instant expiresAt,
        Boolean isExpired,
        
        // Large-scale progress
        Double completionPercentage,
        Integer completedLessons,
        Integer totalLessons,
        
        // Course structure with progress
        List<StudentSectionHomeResponse> sections,
        
        // Course-level metadata
        Integer liveSessions,
        Integer quizzes,
        String estimatedDurationHours,
        
        // Certificate
        Boolean hasCertificate,
        String certificateCode
) {}
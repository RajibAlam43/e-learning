package com.gii.api.model.response.instructor;

import com.gii.common.enums.LiveClassStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record InstructorUpcomingLiveClassResponse(
        UUID liveClassId,
        String title,
        String description,
        
        // Schedule
        Instant startsAt,
        Instant endsAt,
        String timeLabel,  // e.g., "Tomorrow at 2:00 PM"
        
        // Course/lesson context
        UUID courseId,
        String courseName,
        String sectionTitle,
        String lessonTitle,
        
        // Status & registration
        LiveClassStatus status,
        Integer registeredStudents,
        Integer maxCapacity,  // If any limit
        
        // Quick action
        String startUrl,  // Direct link to start teaching
        String detailsUrl
) {}
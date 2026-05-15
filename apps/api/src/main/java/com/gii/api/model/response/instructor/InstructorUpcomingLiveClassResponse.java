package com.gii.api.model.response.instructor;

import com.gii.common.enums.LiveClassStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record InstructorUpcomingLiveClassResponse(
    UUID liveClassId,
    String title,
    String description,

    // Schedule
    Instant startsAt,
    Instant endsAt,
    String timeLabel, // e.g., "Tomorrow at 2:00 PM"

    // Course/section context
    UUID courseId,
    String courseName,
    UUID sectionId,
    String sectionTitle,

    // Status & registration
    LiveClassStatus status,
    Integer registeredStudents,
    Integer maxCapacity, // If any limit

    // Quick action
    String startUrl, // Direct link to start teaching
    String detailsUrl) {}

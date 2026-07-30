package com.gii.api.model.response.instructor;

import com.gii.common.enums.LiveClassStatus;
import com.gii.common.enums.LiveClassProvider;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record InstructorLiveClassResponse(
    UUID liveClassId,
    String title,
    String description,

    // Course/section mapping
    UUID courseId,
    String courseName,
    UUID sectionId,
    String sectionTitle,

    // Instructor info
    String instructorName,
    String instructorEmail,

    // Schedule
    Instant startsAt,
    Instant endsAt,
    Long durationMinutes,
    String timezone, // Instructor's timezone

    // Status
    LiveClassStatus status,
    LiveClassProvider provider,

    // Meeting provider details
    String meetingId,
    String hostStartUrl,
    String joinUrl,

    // Registration & attendance
    Integer registeredStudents,
    Integer attendedStudents,
    List<LiveClassRegistrantSummaryResponse> registrants,

    // Recording (if available)
    Boolean hasRecording,
    String recordingUrl,
    Instant recordingAvailableAt,

    // Timestamps
    Instant createdAt,
    Instant updatedAt) {}

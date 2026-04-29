package com.gii.api.model.response.instructor;

import com.gii.common.enums.LiveClassStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record InstructorLiveClassResponse(
        UUID liveClassId,
        String title,
        String description,
        
        // Course/section/lesson mapping
        UUID courseId,
        String courseName,
        UUID sectionId,
        String sectionTitle,
        UUID lessonId,
        String lessonTitle,
        
        // Instructor info
        String instructorName,
        String instructorEmail,
        
        // Schedule
        Instant startsAt,
        Instant endsAt,
        Long durationMinutes,
        String timezone,  // Instructor's timezone
        
        // Status
        LiveClassStatus status,
        
        // Zoom integration
        String zoomMeetingId,
        String zoomStartUrl,  // For instructor to start
        String zoomJoinUrl,  // For participants to join
        
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
        Instant updatedAt
) {}
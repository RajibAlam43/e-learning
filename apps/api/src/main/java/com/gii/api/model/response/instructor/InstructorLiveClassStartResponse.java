package com.gii.api.model.response.instructor;

import com.gii.common.enums.LiveClassStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record InstructorLiveClassStartResponse(
        UUID liveClassId,
        String title,
        
        // Zoom host/start URLs
        String zoomStartUrl,  // Direct URL for instructor to start meeting
        String zoomMeetingId,  // Meeting ID for reference
        String zoomPassword,  // If password-protected
        
        // Schedule
        Instant startsAt,
        Instant endsAt,
        Long durationMinutes,
        
        // Status confirmation
        LiveClassStatus status,  // Updated to LIVE or SCHEDULED
        
        // Registrant info for instructor
        Integer registeredStudents,
        Integer approvedStudents,
        Integer waitlistedStudents,
        
        // Recording info
        Boolean recordingEnabled,
        String recordingPlaybackUrl,  // If already recording
        
        // Support
        String supportUrl,
        String helpEmail
) {}
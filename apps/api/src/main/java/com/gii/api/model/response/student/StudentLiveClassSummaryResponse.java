package com.gii.api.model.response.student;

import com.gii.common.enums.LiveClassStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record StudentLiveClassSummaryResponse(
        UUID liveClassId,
        String title,
        String description,
        
        // Instructor info
        String instructorName,
        String instructorImageUrl,
        
        // Schedule
        Instant startsAt,
        Instant endsAt,
        Long durationMinutes,  // Calculated from start/end
        String timeZoneLabel,  // e.g., "2:00 PM - 3:00 PM (BDT)"
        
        // Course/section context
        UUID courseId,
        String courseName,
        UUID lessonId,
        String lessonTitle,
        
        // Status
        LiveClassStatus status,  // SCHEDULED, LIVE, COMPLETED, CANCELLED, FAILED
        String statusLabel,  // "Upcoming", "Live Now", "Completed", etc.
        Boolean isLive,  // True if currently streaming
        
        // Registration/join status
        Boolean isRegistered,
        Boolean canJoin,  // Based on status and enrollment
        String joinUrl,  // Pre-generated join link (if appropriate)
        
        // Recording (if available)
        Boolean hasRecording,
        String recordingUrl  // If available
) {}
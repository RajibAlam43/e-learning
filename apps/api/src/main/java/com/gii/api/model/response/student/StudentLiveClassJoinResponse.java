package com.gii.api.model.response.student;

import com.gii.common.enums.LiveClassStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record StudentLiveClassJoinResponse(
    UUID liveClassId,
    String title,

    // Live class details
    LiveClassStatus status,
    Instant startsAt,
    Instant endsAt,

    // Join information
    String zoomJoinUrl, // Direct Zoom join link
    String zoomMeetingId, // For manual entry if needed

    // Instructor info
    String instructorName,
    String instructorEmail,

    // Registration confirmation
    Boolean isRegistered,
    String participantEmail,
    String zoomRegistrantId,

    // Fallback/support
    String supportEmail, // Contact if join fails

    // Recording availability
    Boolean recordingAvailable,
    String recordingUrl // If class is completed/recorded
) {}

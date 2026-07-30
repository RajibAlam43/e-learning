package com.gii.api.model.response.student;

import com.gii.common.enums.LiveClassStatus;
import com.gii.common.enums.LiveClassProvider;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record StudentLiveClassJoinResponse(
    UUID liveClassId,
    String title,

    // Live class details
    LiveClassStatus status,
    LiveClassProvider provider,
    Instant startsAt,
    Instant endsAt,

    // Join information
    String joinUrl,
    String meetingId,

    // Instructor info
    String instructorName,
    String instructorEmail,

    // Registration confirmation
    Boolean isRegistered,
    String participantEmail,
    String providerRegistrantId,

    // Fallback/support
    String supportEmail, // Contact if join fails

    // Recording availability
    Boolean recordingAvailable,
    String recordingUrl // If class is completed/recorded
) {}

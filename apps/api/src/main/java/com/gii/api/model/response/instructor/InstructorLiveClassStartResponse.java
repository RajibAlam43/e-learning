package com.gii.api.model.response.instructor;

import com.gii.common.enums.LiveClassProvider;
import com.gii.common.enums.LiveClassStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record InstructorLiveClassStartResponse(
    UUID liveClassId,
    String title,
    LiveClassProvider provider,
    String hostStartUrl,
    String meetingId,

    // Schedule
    Instant startsAt,
    Instant endsAt,
    Long durationMinutes,

    // Status confirmation
    LiveClassStatus status, // Updated to LIVE or SCHEDULED

    // Registrant info for instructor
    Integer registeredStudents,
    Integer approvedStudents,
    Integer waitlistedStudents,

    // Recording info
    Boolean recordingEnabled,
    String recordingPlaybackUrl, // If already recording

    // Support
    String supportUrl,
    String helpEmail) {}

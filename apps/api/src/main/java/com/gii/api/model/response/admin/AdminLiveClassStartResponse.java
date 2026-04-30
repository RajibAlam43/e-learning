package com.gii.api.model.response.admin;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminLiveClassStartResponse(
    UUID liveClassId,
    String title,
    String zoomStartUrl,
    String zoomMeetingId,
    String zoomPassword,
    Instant startsAt,
    Instant endsAt,
    String status,
    Integer registeredStudents,
    Boolean recordingEnabled) {}

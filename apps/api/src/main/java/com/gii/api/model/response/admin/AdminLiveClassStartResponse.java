package com.gii.api.model.response.admin;

import com.gii.common.enums.LiveClassProvider;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminLiveClassStartResponse(
    UUID liveClassId,
    String title,
    LiveClassProvider provider,
    String hostStartUrl,
    String meetingId,
    Instant startsAt,
    Instant endsAt,
    String status,
    Integer approvedRegistrants,
    Boolean recordingEnabled) {}

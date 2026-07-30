package com.gii.api.model.response.live;

import com.gii.common.enums.LiveClassProvider;
import com.gii.common.enums.LiveClassStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record LiveClassStartResponse(
    UUID liveClassId,
    String title,
    LiveClassProvider provider,
    String hostStartUrl,
    String meetingId,
    Instant startsAt,
    Instant endsAt,
    LiveClassStatus status,
    Integer approvedRegistrants,
    Boolean recordingEnabled) {}

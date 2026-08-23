package com.gii.api.model.response.student;

import com.gii.common.enums.LiveClassProvider;
import com.gii.common.enums.LiveClassStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record StudentLiveClassHomeResponse(
    UUID liveClassId,
    String title,
    String description,
    Instant startsAt,
    Instant endsAt,
    LiveClassProvider provider,
    LiveClassStatus status,
    Boolean attended,
    Boolean completed) {}

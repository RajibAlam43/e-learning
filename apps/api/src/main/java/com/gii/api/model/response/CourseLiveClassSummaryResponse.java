package com.gii.api.model.response;

import com.gii.common.enums.LiveClassProvider;
import com.gii.common.enums.LiveClassStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CourseLiveClassSummaryResponse(
    UUID id,
    String title,
    String description,
    Integer expectedDurationMinutes,
    Boolean isMandatory,
    Boolean scheduled,
    Instant startsAt,
    Instant endsAt,
    LiveClassProvider provider,
    LiveClassStatus status) {}

package com.gii.api.service.live;

import com.gii.common.enums.LiveClassProvider;
import java.time.Instant;
import lombok.Builder;

@Builder
public record LiveMeetingCreateRequest(
    LiveClassProvider provider,
    String title,
    String description,
    Instant startsAt,
    Instant endsAt,
    Integer maxCapacity) {}

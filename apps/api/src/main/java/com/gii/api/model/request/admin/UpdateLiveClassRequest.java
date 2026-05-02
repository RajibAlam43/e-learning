package com.gii.api.model.request.admin;

import java.time.Instant;
import lombok.Builder;

@Builder
public record UpdateLiveClassRequest(
    String title,
    String description,
    Instant startsAt,
    Instant endsAt,
    String status // SCHEDULED, LIVE, COMPLETED, CANCELLED, FAILED
    ) {}

package com.gii.api.model.request.admin;

import lombok.Builder;

import java.time.Instant;

@Builder
public record UpdateLiveClassRequest(
        String title,
        String description,
        Instant startsAt,
        Instant endsAt,
        String status  // SCHEDULED, LIVE, COMPLETED, CANCELLED, FAILED
) {}
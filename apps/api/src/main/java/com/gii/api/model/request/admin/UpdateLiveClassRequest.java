package com.gii.api.model.request.admin;

import jakarta.validation.constraints.Positive;
import java.time.Instant;
import lombok.Builder;

@Builder
public record UpdateLiveClassRequest(
    @Positive Integer position,
    String title,
    String titleEn,
    String description,
    String descriptionEn,
    Instant startsAt,
    Instant endsAt,
    String status // SCHEDULED, LIVE, COMPLETED, CANCELLED, FAILED
    ) {}

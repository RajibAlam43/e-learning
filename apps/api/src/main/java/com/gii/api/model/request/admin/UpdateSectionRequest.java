package com.gii.api.model.request.admin;

import lombok.Builder;

import java.time.Instant;

@Builder
public record UpdateSectionRequest(
        String title,
        String slug,
        Integer position,
        String description,
        Boolean isMandatory,
        Boolean isFree,
        String releaseType,
        Instant releaseAt,
        Integer unlockAfterDays
) {}
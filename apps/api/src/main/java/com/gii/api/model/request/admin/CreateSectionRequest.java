package com.gii.api.model.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.Instant;

@Builder
public record CreateSectionRequest(
        @NotBlank String title,
        @NotBlank String slug,
        @NotNull Integer position,
        String description,
        Boolean isMandatory,
        Boolean isFree,
        String releaseType,  // IMMEDIATE, FIXED_DATE, RELATIVE_DAYS
        Instant releaseAt,
        Integer unlockAfterDays
) {}
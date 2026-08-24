package com.gii.api.model.request.admin;

import com.gii.common.enums.StudyMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;

/** Offering-specific data for another run of an existing course curriculum. */
@Builder
public record RepeatCourseRequest(
    @NotBlank String slug,
    @NotNull BigDecimal priceBdt,
    @NotNull StudyMode studyMode,
    Boolean isFree,
    @Size(max = 80) String timezone,
    Instant enrollmentStartsAt,
    Instant enrollmentEndsAt,
    Instant startsAt,
    Instant endsAt,
    @Positive Integer capacity,
    @Positive Integer accessDurationDays) {}

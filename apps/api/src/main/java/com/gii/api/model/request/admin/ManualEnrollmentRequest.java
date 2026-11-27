package com.gii.api.model.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ManualEnrollmentRequest(
    @NotNull UUID requestId,
    UUID courseId,
    UUID programId,
    boolean programFunded,
    boolean overrideDates,
    boolean overrideCapacity,
    boolean allowUnpublished,
    @NotBlank @Size(max = 5000) String reason) {}

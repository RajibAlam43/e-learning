package com.gii.api.model.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record ManualEnrollmentRequest(
    @NotNull UUID requestId,
    UUID courseId,
    UUID programId,
    boolean programFunded,
    boolean overrideDates,
    boolean overrideCapacity,
    boolean allowUnpublished,
    @NotBlank @Size(max = 5000) String reason,
    Instant expiresAt) {
  public ManualEnrollmentRequest {
    expiresAt =
        expiresAt == null ? null : expiresAt.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
  }

  public ManualEnrollmentRequest(
      UUID requestId,
      UUID courseId,
      UUID programId,
      boolean programFunded,
      boolean overrideDates,
      boolean overrideCapacity,
      boolean allowUnpublished,
      String reason) {
    this(
        requestId,
        courseId,
        programId,
        programFunded,
        overrideDates,
        overrideCapacity,
        allowUnpublished,
        reason,
        null);
  }
}

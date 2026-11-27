package com.gii.api.model.response.academic;

import java.time.Instant;
import java.util.UUID;

public record ManualEnrollmentResponse(
    UUID id,
    UUID requestId,
    UUID userId,
    UUID actorId,
    String actorName,
    UUID courseId,
    String courseTitle,
    UUID programId,
    String programTitle,
    boolean programFunded,
    boolean overrideDates,
    boolean overrideCapacity,
    boolean allowUnpublished,
    String reason,
    UUID enrollmentId,
    UUID registrationId,
    Instant accessExpiresAt,
    boolean changed,
    Instant createdAt,
    Instant requestedExpiresAt) {}

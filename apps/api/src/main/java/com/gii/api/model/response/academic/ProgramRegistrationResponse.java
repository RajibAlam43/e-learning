package com.gii.api.model.response.academic;

import com.gii.common.enums.ProgramRegistrationStatus;
import java.time.Instant;
import java.util.UUID;

public record ProgramRegistrationResponse(
    UUID id,
    UUID programId,
    String programTitle,
    UUID courseTemplateId,
    UUID courseId,
    String courseTitle,
    String cohortName,
    int credits,
    boolean mandatory,
    ProgramRegistrationStatus status,
    Instant registeredAt,
    Instant accessExpiresAt,
    Instant withdrawnAt,
    String withdrawnByName,
    String withdrawalReason,
    boolean canWithdraw) {}

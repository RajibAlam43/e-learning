package com.gii.api.model.response.academic;

import com.gii.common.enums.WaiverStatus;
import java.time.Instant;
import java.util.UUID;

public record WaiverResponse(
    UUID id,
    UUID userId,
    String studentName,
    String studentContact,
    UUID courseTemplateId,
    String courseTitle,
    int requestedCredits,
    int currentCredits,
    String reason,
    boolean hasEvidence,
    WaiverStatus status,
    String reviewReason,
    String reviewerName,
    Instant reviewedAt,
    Instant createdAt,
    UUID achievementId,
    Integer awardedCredits) {}

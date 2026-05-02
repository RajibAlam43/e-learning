package com.gii.api.model.response.student;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record StudentCertificateSummaryResponse(
    UUID certificateId,
    String certificateCode, // Unique verification code

    // Course info
    String courseName,
    String courseSlug,

    // Recipient info
    String recipientName,

    // Certificate details
    Instant issuedAt,
    Boolean isRevoked,
    Instant revokedAt, // Null if not revoked

    // Access
    String pdfUrl, // Download link
    String verificationUrl // Public verification page URL
    ) {}

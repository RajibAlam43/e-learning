package com.gii.api.model.response.student;

import com.gii.common.enums.CertificateTargetType;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record StudentCertificateSummaryResponse(
    UUID certificateId,
    String certificateCode, // Unique verification code

    // Target info
    CertificateTargetType targetType,
    String targetName,
    String targetSlug,

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

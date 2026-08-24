package com.gii.api.model.response.certificate;

import com.gii.common.enums.CertificateTargetType;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CertificateDetailResponse(
    UUID certificateId,
    String certificateCode,
    String recipientName,
    CertificateTargetType targetType,
    String targetName,
    String targetSlug,
    String instructorName,
    Instant issuedAt,
    Boolean isRevoked,
    UUID templateId,
    String templateName,
    String storageProvider,
    String storageBucket,
    String objectKey,
    String storageLocation,
    String legacyPdfUrl,
    String downloadEndpoint,
    String verificationUrl) {}

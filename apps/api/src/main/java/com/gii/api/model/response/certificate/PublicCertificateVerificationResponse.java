package com.gii.api.model.response.certificate;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record PublicCertificateVerificationResponse(
        Boolean isValid,  // Whether certificate code is valid
        
        // Certificate details (if valid)
        UUID certificateId,
        String certificateCode,
        String recipientName,
        String courseName,
        String courseSlug,
        String instructorName,
        
        // Issuance and validity
        Instant issuedAt,
        Boolean isRevoked,
        Instant revokedAt,  // Null if not revoked
        
        // Verification metadata
        String verificationStatus,  // "VALID", "INVALID", "REVOKED", "EXPIRED"
        String verificationMessage,  // e.g., "Certificate is valid and issued on [date]"
        
        // Public display info
        String certificateImageUrl,  // Optional: public certificate image
        String issuerName,  // Company/organization name
        String issuerLogoUrl,  // Company logo
        
        // Additional context
        String completionCriteria,  // e.g., "Completed all lessons with 80% quiz score"
        Double completionPercentage  // e.g., 95.5
) {}
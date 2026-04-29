package com.gii.api.model.response.certificate;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record CertificateIssueResponse(
        UUID certificateId,
        String certificateCode,  // Unique verification code
        
        // Certificate details
        String recipientName,
        String courseName,
        String courseSlug,
        String instructorName,
        
        // Issuance info
        Instant issuedAt,
        Boolean isRevoked,
        Instant revokedAt,  // Null if not revoked
        
        // Access
        String pdfUrl,  // Direct download URL (if available)
        String downloadUrl,  // Signed temporary download URL
        Instant downloadUrlExpiresAt,  // When signed URL expires
        
        // Verification
        String verificationUrl,  // Public verification page URL
        
        // Eligibility check
        Boolean wasEligible,  // Whether user met completion criteria
        String eligibilityReason,  // e.g., "COURSE_COMPLETED", "MINIMUM_SCORE_NOT_MET"
        
        // Message
        String message  // e.g., "Certificate issued successfully!"
) {}
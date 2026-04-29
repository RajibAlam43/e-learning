package com.gii.api.controller;

import com.gii.api.model.response.CertificateDownloadUrlResponse;
import com.gii.api.model.response.CertificateIssueResponse;
import com.gii.api.model.response.PublicCertificateVerificationResponse;
import com.gii.api.processor.CertificateApiProcessingService;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CertificateApiController {

    private final CertificateApiProcessingService certificateApiProcessingService;

    /**
     * Checks certificate eligibility for the current student and issues a certificate
     * if eligible. If a certificate already exists, returns the existing one.
     * This endpoint should be implemented as idempotent.
     *
     * @param courseId ID of the course for certificate issuance
     * @param authentication authenticated user context from Spring Security
     * @return issued or existing certificate payload for the course
     */
    @PostMapping("/student/courses/{courseId}/certificate")
    public ResponseEntity<@NotNull CertificateIssueResponse> issueOrGetCertificate(
            @PathVariable UUID courseId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                certificateApiProcessingService.issueOrGetCertificate(courseId, authentication)
        );
    }

    /**
     * Returns a signed download URL (or download metadata) for a student's certificate
     * after access checks.
     *
     * @param certificateId ID of the certificate to download
     * @param authentication authenticated user context from Spring Security
     * @return signed certificate download URL payload
     */
    @GetMapping("/student/certificates/{certificateId}/download")
    public ResponseEntity<@NotNull CertificateDownloadUrlResponse> getCertificateDownloadUrl(
            @PathVariable UUID certificateId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                certificateApiProcessingService.getCertificateDownloadUrl(certificateId, authentication)
        );
    }

    /**
     * Publicly verifies a certificate by verification code.
     *
     * @param code public verification code printed on the certificate
     * @return public verification payload indicating certificate validity
     */
    @GetMapping("/public/certificates/verify/{code}")
    public ResponseEntity<@NotNull PublicCertificateVerificationResponse> verifyCertificate(
            @PathVariable String code
    ) {
        return ResponseEntity.ok(certificateApiProcessingService.verifyCertificate(code));
    }
}

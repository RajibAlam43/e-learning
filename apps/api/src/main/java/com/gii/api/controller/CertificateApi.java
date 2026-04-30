package com.gii.api.controller;

import com.gii.api.model.response.certificate.CertificateDownloadUrlResponse;
import com.gii.api.model.response.certificate.CertificateIssueResponse;
import com.gii.api.model.response.certificate.PublicCertificateVerificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Tag(name = "Certificates", description = "Certificate issuance and public verification")
public interface CertificateApi {

  @PostMapping("/student/courses/{courseId}/certificate")
  @Operation(
      summary = "Issue or get certificate",
      description =
          "Check eligibility and issue certificate if qualified; return existing if already"
              + " issued. Idempotent operation.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Certificate issued or retrieved",
            content = @Content(schema = @Schema(implementation = CertificateIssueResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Not eligible for certificate"),
        @ApiResponse(responseCode = "404", description = "Course not found")
      })
  ResponseEntity<CertificateIssueResponse> issueOrGetCertificate(
      @PathVariable UUID courseId, Authentication authentication);

  @GetMapping("/student/certificates/{certificateId}/download")
  @Operation(
      summary = "Get certificate download URL",
      description = "Get signed temporary download URL for certificate PDF.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Download URL retrieved",
            content =
                @Content(schema = @Schema(implementation = CertificateDownloadUrlResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Not certificate owner"),
        @ApiResponse(responseCode = "404", description = "Certificate not found")
      })
  ResponseEntity<CertificateDownloadUrlResponse> getCertificateDownloadUrl(
      @PathVariable UUID certificateId, Authentication authentication);

  @GetMapping("/public/certificates/verify/{code}")
  @Operation(
      summary = "Verify certificate",
      description = "Publicly verify a certificate using its verification code.",
      security = {})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Verification result retrieved",
            content =
                @Content(
                    schema =
                        @Schema(implementation = PublicCertificateVerificationResponse.class))),
        @ApiResponse(responseCode = "404", description = "Certificate not found")
      })
  ResponseEntity<PublicCertificateVerificationResponse> verifyCertificate(
      @PathVariable String code);
}

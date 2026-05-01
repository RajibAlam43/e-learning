package com.gii.api.controller;

import com.gii.api.model.response.certificate.CertificateDownloadUrlResponse;
import com.gii.api.model.response.certificate.CertificateIssueResponse;
import com.gii.api.model.response.certificate.PublicCertificateVerificationResponse;
import com.gii.api.service.certificate.CertificateDownloadService;
import com.gii.api.service.certificate.CertificateIssueService;
import com.gii.api.service.certificate.CertificateVerificationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CertificateApiController implements CertificateApi {

  private final CertificateIssueService certificateIssueService;
  private final CertificateDownloadService certificateDownloadService;
  private final CertificateVerificationService certificateVerificationService;

  @Override
  public ResponseEntity<CertificateIssueResponse> issueOrGetCertificate(
      UUID courseId, Authentication authentication) {
    return ResponseEntity.ok(certificateIssueService.execute(courseId, authentication));
  }

  @Override
  public ResponseEntity<CertificateDownloadUrlResponse> getCertificateDownloadUrl(
      UUID certificateId, Authentication authentication) {
    return ResponseEntity.ok(certificateDownloadService.execute(certificateId, authentication));
  }

  @Override
  public ResponseEntity<PublicCertificateVerificationResponse> verifyCertificate(String code) {
    return ResponseEntity.ok(certificateVerificationService.execute(code));
  }
}

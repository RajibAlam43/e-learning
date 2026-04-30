package com.gii.api.service.certificate;

import com.gii.api.model.response.certificate.CertificateDownloadUrlResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.api.service.storage.R2PresignedUrlService;
import com.gii.common.entity.certificate.Certificate;
import com.gii.common.repository.certificate.CertificateRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CertificateDownloadService {

  private final CurrentUserService currentUserService;
  private final CertificateRepository certificateRepository;
  private final R2PresignedUrlService r2PresignedUrlService;

  public CertificateDownloadUrlResponse execute(UUID certificateId, Authentication authentication) {
    UUID userId = currentUserService.getCurrentUserId(authentication);
    Certificate certificate =
        certificateRepository
            .findByIdAndUserId(certificateId, userId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate not found"));
    if (certificate.getRevokedAt() != null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Certificate has been revoked");
    }
    if (certificate.getPdfUrl() == null || certificate.getPdfUrl().isBlank()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate PDF is not available");
    }

    var signed =
        r2PresignedUrlService.generateDownloadUrl(
            certificate.getPdfUrl(),
            "Certificate-" + certificate.getCourse().getSlug() + ".pdf",
            "application/pdf");

    return CertificateDownloadUrlResponse.builder()
        .downloadUrl(signed.downloadUrl())
        .expiresAt(signed.expiresAt())
        .fileName("Certificate-" + certificate.getCourse().getSlug() + ".pdf")
        .contentType("application/pdf")
        .fileSizeBytes(null)
        .checksum(null)
        .build();
  }
}

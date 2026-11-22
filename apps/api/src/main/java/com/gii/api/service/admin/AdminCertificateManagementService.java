package com.gii.api.service.admin;

import com.gii.api.model.request.admin.RevokeCertificateRequest;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.entity.certificate.Certificate;
import com.gii.common.repository.certificate.CertificateRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminCertificateManagementService {

  private final CertificateRepository certificateRepository;
  private final CurrentUserService currentUserService;

  public void revoke(
      UUID certificateId, RevokeCertificateRequest request, Authentication authentication) {
    Certificate certificate =
        certificateRepository
            .findByIdForUpdate(certificateId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate not found"));
    if (certificate.getRevokedAt() != null) {
      return;
    }

    certificate.setRevokedAt(Instant.now());
    certificate.setRevokedBy(currentUserService.getCurrentUser(authentication));
    certificate.setRevocationReason(normalizeReason(request.reason()));
    certificateRepository.save(certificate);
  }

  public void reinstate(UUID certificateId) {
    Certificate certificate =
        certificateRepository
            .findByIdForUpdate(certificateId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate not found"));
    if (certificate.getRevokedAt() == null) {
      return;
    }
    certificate.setRevokedAt(null);
    certificate.setRevokedBy(null);
    certificate.setRevocationReason(null);
    certificateRepository.save(certificate);
  }

  private String normalizeReason(String reason) {
    if (reason == null || reason.isBlank()) {
      return null;
    }
    return reason.trim();
  }
}

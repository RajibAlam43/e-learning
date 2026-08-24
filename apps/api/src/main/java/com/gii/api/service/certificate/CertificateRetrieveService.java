package com.gii.api.service.certificate;

import com.gii.api.model.response.certificate.CertificateDetailResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.api.service.storage.R2ObjectStorageService;
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
public class CertificateRetrieveService {

  private static final String VERIFICATION_BASE_PATH = "/public/certificates/verify/";

  private final CurrentUserService currentUserService;
  private final CertificateRepository certificateRepository;
  private final R2ObjectStorageService objectStorageService;

  public CertificateDetailResponse execute(UUID certificateId, Authentication authentication) {
    UUID userId = currentUserService.getCurrentUserId(authentication);
    Certificate certificate =
        certificateRepository
            .findByIdAndUserId(certificateId, userId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate not found"));

    String objectKey = certificate.getPdfObjectKey();
    return CertificateDetailResponse.builder()
        .certificateId(certificate.getId())
        .certificateCode(certificate.getCertificateCode())
        .recipientName(certificate.getRecipientName())
        .targetType(certificate.getTargetType())
        .targetName(certificate.getTargetTitle())
        .targetSlug(certificate.getTargetSlug())
        .instructorName(certificate.getInstructorName())
        .issuedAt(certificate.getIssuedAt())
        .isRevoked(certificate.getRevokedAt() != null)
        .templateId(certificate.getTemplate() == null ? null : certificate.getTemplate().getId())
        .templateName(
            certificate.getTemplate() == null ? null : certificate.getTemplate().getName())
        .storageProvider(objectKey == null ? null : "R2")
        .storageBucket(objectKey == null ? null : objectStorageService.bucket())
        .objectKey(objectKey)
        .storageLocation(objectKey == null ? null : objectStorageService.storageLocation(objectKey))
        .legacyPdfUrl(certificate.getPdfUrl())
        .downloadEndpoint("/student/certificates/" + certificate.getId() + "/download")
        .verificationUrl(VERIFICATION_BASE_PATH + certificate.getCertificateCode())
        .build();
  }
}

package com.gii.api.service.student;

import com.gii.api.model.response.student.StudentCertificateSummaryResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.entity.certificate.Certificate;
import com.gii.common.repository.certificate.CertificateRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentCertificatesService {

  private static final String VERIFICATION_BASE_PATH = "/public/certificates/verify/";

  private final CurrentUserService currentUserService;
  private final CertificateRepository certificateRepository;

  public List<StudentCertificateSummaryResponse> execute(Authentication authentication) {
    java.util.UUID userId = currentUserService.getCurrentUserId(authentication);
    List<Certificate> certificates = certificateRepository.findByUserIdOrderByIssuedAtDesc(userId);

    return certificates.stream().map(this::toCertificateSummary).toList();
  }

  private StudentCertificateSummaryResponse toCertificateSummary(Certificate certificate) {
    return StudentCertificateSummaryResponse.builder()
        .certificateId(certificate.getId())
        .certificateCode(certificate.getCertificateCode())
        .courseName(certificate.getCourseTitle())
        .courseSlug(certificate.getCourse().getSlug())
        .recipientName(certificate.getRecipientName())
        .issuedAt(certificate.getIssuedAt())
        .isRevoked(certificate.getRevokedAt() != null)
        .revokedAt(certificate.getRevokedAt())
        .pdfUrl(certificate.getPdfUrl())
        .verificationUrl(VERIFICATION_BASE_PATH + certificate.getCertificateCode())
        .build();
  }
}

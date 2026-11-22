package com.gii.api.service.certificate;

import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.repository.certificate.CertificateRepository;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Certificates aren't generated automatically the instant an enrollment completes (see {@link
 * com.gii.api.service.progress.EnrollmentCompletionService}), so this fills in any missing ones
 * lazily. Called from the "list my certificates" read path before returning results, so a
 * completed learner always sees a certificate without needing a dedicated "issue" action in the
 * client. Each candidate is issued in its own transaction (see {@link
 * CertificateIssueService#tryIssueCourseCertificate} / {@link
 * CertificateIssueService#tryIssueCollectionCertificate}) so one failure can't block the rest.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateBackfillService {

  private final EnrollmentRepository enrollmentRepository;
  private final CollectionEnrollmentRepository collectionEnrollmentRepository;
  private final CertificateRepository certificateRepository;
  private final CertificateIssueService certificateIssueService;

  public void backfillMissingCertificates(UUID userId) {
    for (Enrollment enrollment :
        enrollmentRepository.findByUserIdAndStatusAndCompletedAtIsNotNull(
            userId, EnrollmentStatus.ACTIVE)) {
      if (certificateRepository
          .findByUserIdAndCourseId(userId, enrollment.getCourse().getId())
          .isPresent()) {
        continue;
      }
      try {
        certificateIssueService.tryIssueCourseCertificate(enrollment.getId());
      } catch (RuntimeException failure) {
        log.error(
            "Failed to auto-issue certificate for user {} course {}",
            userId,
            enrollment.getCourse().getId(),
            failure);
      }
    }

    for (CollectionEnrollment enrollment :
        collectionEnrollmentRepository.findByUserIdAndStatusAndCompletedAtIsNotNull(
            userId, EnrollmentStatus.ACTIVE)) {
      UUID collectionId = enrollment.getCollection().getId();
      if (certificateRepository.findByUserIdAndCollectionId(userId, collectionId).isPresent()) {
        continue;
      }
      try {
        certificateIssueService.tryIssueCollectionCertificate(enrollment.getId());
      } catch (RuntimeException failure) {
        log.error(
            "Failed to auto-issue certificate for user {} collection {}",
            userId,
            collectionId,
            failure);
      }
    }
  }
}

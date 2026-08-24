package com.gii.api.service.certificate;

import com.gii.api.model.response.certificate.CertificateIssueResponse;
import com.gii.api.service.certificate.CertificateDocumentService.StoredCertificateDocument;
import com.gii.api.service.collection.PurchasedCollectionCoursesService;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.api.service.localization.LocalizedContentService;
import com.gii.api.service.progress.CourseCompletionService;
import com.gii.api.service.progress.CourseCompletionService.CourseCompletion;
import com.gii.api.service.storage.R2ObjectStorageService;
import com.gii.common.entity.certificate.Certificate;
import com.gii.common.entity.collection.Collection;
import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.entity.course.CourseInstructor;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.user.User;
import com.gii.common.enums.CertificateTargetType;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.InstructorRole;
import com.gii.common.repository.certificate.CertificateRepository;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import com.gii.common.repository.collection.CollectionRepository;
import com.gii.common.repository.course.CourseInstructorRepository;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
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
public class CertificateIssueService {

  private static final String VERIFICATION_BASE_PATH = "/public/certificates/verify/";
  private static final String CODE_PREFIX = "GII-CERT-";
  private static final int CODE_RANDOM_LEN = 10;
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final char[] CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

  private final CurrentUserService currentUserService;
  private final CourseRepository courseRepository;
  private final CollectionRepository collectionRepository;
  private final CollectionEnrollmentRepository collectionEnrollmentRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final CourseCompletionService courseCompletionService;
  private final CertificateRepository certificateRepository;
  private final CourseInstructorRepository courseInstructorRepository;
  private final CertificateDocumentService certificateDocumentService;
  private final R2ObjectStorageService objectStorageService;
  private final LocalizedContentService localizedContentService;
  private final PurchasedCollectionCoursesService purchasedCollectionCoursesService;

  public CertificateIssueResponse executeCourse(UUID courseId, Authentication authentication) {
    User user = currentUserService.getCurrentUser(authentication);
    courseRepository
        .findById(courseId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

    Certificate existing =
        certificateRepository.findByUserIdAndCourseId(user.getId(), courseId).orElse(null);
    if (existing != null) {
      requireNotRevoked(existing);
      ensureDocument(existing, resolveInstructorName(existing), existing.getIssuedAt());
      return toResponse(existing, true, "CERTIFICATE_ALREADY_EXISTS");
    }

    Enrollment enrollment =
        enrollmentRepository
            .findByUserIdAndCourseIdForUpdate(user.getId(), courseId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Not enrolled in this course"));

    // Re-check after lock so concurrent issue requests become idempotent.
    existing = certificateRepository.findByUserIdAndCourseId(user.getId(), courseId).orElse(null);
    if (existing != null) {
      requireNotRevoked(existing);
      ensureDocument(existing, resolveInstructorName(existing), existing.getIssuedAt());
      return toResponse(existing, true, "CERTIFICATE_ALREADY_EXISTS");
    }

    if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Enrollment is not active");
    }
    if (enrollment.getExpiresAt() != null && !enrollment.getExpiresAt().isAfter(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Enrollment has expired");
    }

    boolean eligible = enrollment.getCompletedAt() != null;
    if (!eligible) {
      CourseCompletion completion = courseCompletionService.get(user.getId(), courseId);
      eligible = completion.totalItems() > 0 && completion.completedItems() >= completion.totalItems();
      if (eligible) {
        // The learner just crossed 100% but no progress event has persisted completedAt yet
        // (e.g. this request raced ahead of EnrollmentCompletionService.refresh). Persist it now
        // so completion stays sticky from this point on, same as the normal completion path.
        enrollment.setCompletedAt(Instant.now());
        enrollmentRepository.save(enrollment);
      }
    }

    if (!eligible) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Course completion criteria not met");
    }

    Certificate certificate =
        Certificate.builder()
            .certificateCode(normalizeCode(generateUniqueCode()))
            .user(user)
            .targetType(CertificateTargetType.COURSE)
            .course(enrollment.getCourse())
            .enrollment(enrollment)
            .collection(null)
            .issuedBy(user)
            .recipientName(user.getFullName())
            .targetTitle(
                localizedContentService.english(
                    enrollment.getCourse().getTitle(), enrollment.getCourse().getTitleEn()))
            .targetSlug(enrollment.getCourse().getSlug())
            .instructorName(resolveInstructorName(enrollment.getCourse()))
            .build();
    Certificate saved = certificateRepository.saveAndFlush(certificate);
    ensureDocument(
        saved,
        saved.getInstructorName(),
        enrollment.getCompletedAt() == null ? saved.getIssuedAt() : enrollment.getCompletedAt());

    return toResponse(saved, true, "COURSE_COMPLETED");
  }

  public CertificateIssueResponse executeCollection(
      UUID collectionId, Authentication authentication) {
    User user = currentUserService.getCurrentUser(authentication);
    final Collection collection =
        collectionRepository
            .findById(collectionId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found"));

    Certificate existing =
        certificateRepository.findByUserIdAndCollectionId(user.getId(), collectionId).orElse(null);
    if (existing != null) {
      requireNotRevoked(existing);
      ensureDocument(existing, "Global Islamic Institute", existing.getIssuedAt());
      return toResponse(existing, true, "CERTIFICATE_ALREADY_EXISTS");
    }

    CollectionEnrollment enrollment =
        collectionEnrollmentRepository
            .findByUserIdAndCollectionIdForUpdate(user.getId(), collectionId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Not enrolled in this collection"));

    existing =
        certificateRepository.findByUserIdAndCollectionId(user.getId(), collectionId).orElse(null);
    if (existing != null) {
      requireNotRevoked(existing);
      ensureDocument(existing, "Global Islamic Institute", existing.getIssuedAt());
      return toResponse(existing, true, "CERTIFICATE_ALREADY_EXISTS");
    }

    if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Enrollment is not active");
    }
    if (enrollment.getExpiresAt() != null && !enrollment.getExpiresAt().isAfter(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Enrollment has expired");
    }

    boolean eligible = enrollment.getCompletedAt() != null;
    if (!eligible) {
      var courseIds =
          purchasedCollectionCoursesService.resolveItems(enrollment).stream()
              .filter(
                  com.gii.api.service.collection.PurchasedCollectionCoursesService.PurchasedCourse
                      ::mandatory)
              .map(item -> item.course().getId())
              .distinct()
              .toList();
      if (!courseIds.isEmpty()) {
        var completions = courseCompletionService.getByCourseIds(user.getId(), courseIds);
        int totalItems = completions.values().stream().mapToInt(CourseCompletion::totalItems).sum();
        int completedItems =
            completions.values().stream().mapToInt(CourseCompletion::completedItems).sum();
        eligible = totalItems > 0 && completedItems >= totalItems;
      }
      if (eligible) {
        // Same race as the course path: persist completedAt now so it stays sticky from here on.
        enrollment.setCompletedAt(Instant.now());
        collectionEnrollmentRepository.save(enrollment);
      }
    }
    if (!eligible) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Collection completion criteria not met");
    }

    Certificate certificate =
        Certificate.builder()
            .certificateCode(normalizeCode(generateUniqueCode()))
            .user(user)
            .targetType(CertificateTargetType.COLLECTION)
            .course(null)
            .enrollment(null)
            .collection(collection)
            .collectionEnrollment(enrollment)
            .issuedBy(user)
            .recipientName(user.getFullName())
            .targetTitle(
                localizedContentService.english(collection.getTitle(), collection.getTitleEn()))
            .targetSlug(collection.getSlug())
            .instructorName("Global Islamic Institute")
            .build();
    Certificate saved = certificateRepository.saveAndFlush(certificate);
    requireNotRevoked(saved);
    ensureDocument(
        saved,
        "Global Islamic Institute",
        enrollment.getCompletedAt() == null ? saved.getIssuedAt() : enrollment.getCompletedAt());
    return toResponse(saved, true, "COLLECTION_COMPLETED");
  }

  private void requireNotRevoked(Certificate certificate) {
    if (certificate.getRevokedAt() != null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Certificate has been revoked");
    }
  }

  private CertificateIssueResponse toResponse(
      Certificate certificate, boolean eligible, String eligibilityReason) {
    String instructorName = resolveInstructorName(certificate);
    String objectKey = certificate.getPdfObjectKey();

    return CertificateIssueResponse.builder()
        .certificateId(certificate.getId())
        .certificateCode(certificate.getCertificateCode())
        .recipientName(certificate.getRecipientName())
        .targetType(certificate.getTargetType())
        .targetName(certificate.getTargetTitle())
        .targetSlug(certificate.getTargetSlug())
        .instructorName(instructorName)
        .issuedAt(certificate.getIssuedAt())
        .isRevoked(certificate.getRevokedAt() != null)
        .revokedAt(certificate.getRevokedAt())
        .pdfUrl(certificate.getPdfUrl())
        .objectKey(objectKey)
        .storageLocation(objectKey == null ? null : objectStorageService.storageLocation(objectKey))
        .downloadEndpoint("/student/certificates/" + certificate.getId() + "/download")
        .downloadUrl(null)
        .downloadUrlExpiresAt(null)
        .verificationUrl(VERIFICATION_BASE_PATH + certificate.getCertificateCode())
        .wasEligible(eligible)
        .eligibilityReason(eligibilityReason)
        .message("Certificate issued successfully")
        .build();
  }

  private void ensureDocument(
      Certificate certificate, String instructorName, Instant completionDate) {
    if (certificate.getPdfObjectKey() != null && !certificate.getPdfObjectKey().isBlank()) {
      return;
    }
    String snapshotInstructorName =
        instructorName == null || instructorName.isBlank()
            ? resolveInstructorName(certificate.getCourse())
            : instructorName;
    certificate.setInstructorName(snapshotInstructorName);
    StoredCertificateDocument document =
        certificateDocumentService.generateAndUpload(
            certificate,
            snapshotInstructorName,
            completionDate == null ? Instant.now() : completionDate);
    certificate.setTemplate(document.template());
    certificate.setPdfObjectKey(document.objectKey());
    certificateRepository.saveAndFlush(certificate);
  }

  private String resolveInstructorName(Certificate certificate) {
    if (certificate.getInstructorName() != null && !certificate.getInstructorName().isBlank()) {
      return certificate.getInstructorName();
    }
    return resolveInstructorName(certificate.getCourse());
  }

  private String resolveInstructorName(com.gii.common.entity.course.Course course) {
    if (course == null) {
      return "Global Islamic Institute";
    }
    return courseInstructorRepository.findByCourseId(course.getId()).stream()
        .filter(ci -> ci.getRole() == InstructorRole.PRIMARY)
        .findFirst()
        .or(() -> courseInstructorRepository.findByCourseId(course.getId()).stream().findFirst())
        .map(CourseInstructor::getInstructor)
        .map(User::getFullName)
        .orElse("Instructor");
  }

  private String generateUniqueCode() {
    for (int i = 0; i < 10; i++) {
      String candidate = CODE_PREFIX + randomBlock(CODE_RANDOM_LEN);
      if (certificateRepository.findByCertificateCode(candidate).isEmpty()) {
        return candidate;
      }
    }
    throw new ResponseStatusException(
        HttpStatus.INTERNAL_SERVER_ERROR, "Unable to allocate certificate code");
  }

  private String randomBlock(int len) {
    StringBuilder builder = new StringBuilder(len);
    for (int i = 0; i < len; i++) {
      builder.append(CHARS[RANDOM.nextInt(CHARS.length)]);
    }
    return builder.toString();
  }

  private String normalizeCode(String code) {
    return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
  }
}

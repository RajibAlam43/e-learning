package com.gii.api.service.certificate;

import com.gii.api.model.response.certificate.CertificateIssueResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.api.service.localization.LocalizedContentService;
import com.gii.api.service.storage.R2PresignedUrlService;
import com.gii.common.entity.certificate.Certificate;
import com.gii.common.entity.collection.Collection;
import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.entity.course.CourseInstructor;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.user.User;
import com.gii.common.enums.CertificateTargetType;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.InstructorRole;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.certificate.CertificateRepository;
import com.gii.common.repository.collection.CollectionCourseRepository;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import com.gii.common.repository.collection.CollectionRepository;
import com.gii.common.repository.course.CourseInstructorRepository;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.enrollment.LessonProgressRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
  private final CollectionCourseRepository collectionCourseRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final LessonRepository lessonRepository;
  private final LessonProgressRepository lessonProgressRepository;
  private final CertificateRepository certificateRepository;
  private final CourseInstructorRepository courseInstructorRepository;
  private final R2PresignedUrlService r2PresignedUrlService;
  private final LocalizedContentService localizedContentService;

  public CertificateIssueResponse executeCourse(UUID courseId, Authentication authentication) {
    User user = currentUserService.getCurrentUser(authentication);
    courseRepository
        .findById(courseId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

    Certificate existing =
        certificateRepository.findByUserIdAndCourseId(user.getId(), courseId).orElse(null);
    if (existing != null) {
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
      return toResponse(existing, true, "CERTIFICATE_ALREADY_EXISTS");
    }

    if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Enrollment is not active");
    }
    if (enrollment.getExpiresAt() != null && enrollment.getExpiresAt().isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Enrollment has expired");
    }

    long totalLessons =
        lessonRepository.countByCourseIdAndStatus(courseId, PublishStatus.PUBLISHED);
    long completedLessons =
        lessonProgressRepository.countByUserIdAndLessonCourseIdAndCompletedAtIsNotNull(
            user.getId(), courseId);
    boolean eligible = totalLessons > 0 && completedLessons >= totalLessons;

    if (!eligible) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Course completion criteria not met");
    }

    Certificate certificate =
        Certificate.builder()
            .certificateCode(normalizeCode(generateUniqueCode()))
            .user(user)
            .targetType(CertificateTargetType.COURSE)
            .course(enrollment.getCourse())
            .collection(null)
            .issuedBy(user)
            .recipientName(user.getFullName())
            .targetTitle(
                localizedContentService.english(
                    enrollment.getCourse().getTitle(), enrollment.getCourse().getTitleEn()))
            .targetSlug(enrollment.getCourse().getSlug())
            .build();
    Certificate saved = certificateRepository.save(certificate);

    return toResponse(saved, true, "COURSE_COMPLETED");
  }

  public CertificateIssueResponse executeCollection(
      UUID collectionId, Authentication authentication) {
    User user = currentUserService.getCurrentUser(authentication);
    Collection collection =
        collectionRepository
            .findById(collectionId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found"));

    Certificate existing =
        certificateRepository.findByUserIdAndCollectionId(user.getId(), collectionId).orElse(null);
    if (existing != null) {
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
      return toResponse(existing, true, "CERTIFICATE_ALREADY_EXISTS");
    }

    if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Enrollment is not active");
    }
    if (enrollment.getExpiresAt() != null && enrollment.getExpiresAt().isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Enrollment has expired");
    }

    var collectionCourses =
        collectionCourseRepository.findByCollection_IdOrderByPositionAscWithCourseStatus(
            collectionId, PublishStatus.PUBLISHED);
    var courseIds =
        collectionCourses.stream().map(cc -> cc.getCourse().getId()).distinct().toList();
    if (courseIds.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Collection completion criteria not met");
    }

    long totalLessons = 0;
    for (Object[] row :
        lessonRepository.countByCourseIdsAndStatus(courseIds, PublishStatus.PUBLISHED)) {
      totalLessons += (Long) row[1];
    }
    long completedLessons = 0;
    for (Object[] row :
        lessonProgressRepository.countCompletedByUserIdAndCourseIds(user.getId(), courseIds)) {
      completedLessons += (Long) row[1];
    }

    boolean eligible = totalLessons > 0 && completedLessons >= totalLessons;
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
            .collection(collection)
            .issuedBy(user)
            .recipientName(user.getFullName())
            .targetTitle(
                localizedContentService.english(collection.getTitle(), collection.getTitleEn()))
            .targetSlug(collection.getSlug())
            .build();
    Certificate saved =
        saveCollectionCertificateIdempotent(certificate, user.getId(), collectionId);
    return toResponse(saved, true, "COLLECTION_COMPLETED");
  }

  private Certificate saveCollectionCertificateIdempotent(
      Certificate certificate, UUID userId, UUID collectionId) {
    try {
      return certificateRepository.save(certificate);
    } catch (DataIntegrityViolationException ex) {
      return certificateRepository
          .findByUserIdAndCollectionId(userId, collectionId)
          .orElseThrow(() -> ex);
    }
  }

  private CertificateIssueResponse toResponse(
      Certificate certificate, boolean eligible, String eligibilityReason) {
    String instructorName = null;
    if (certificate.getTargetType() == CertificateTargetType.COURSE
        && certificate.getCourse() != null) {
      instructorName =
          courseInstructorRepository.findByCourseId(certificate.getCourse().getId()).stream()
              .filter(ci -> ci.getRole() == InstructorRole.PRIMARY)
              .findFirst()
              .or(
                  () ->
                      courseInstructorRepository
                          .findByCourseId(certificate.getCourse().getId())
                          .stream()
                          .findFirst())
              .map(CourseInstructor::getInstructor)
              .map(User::getFullName)
              .orElse("Instructor");
    }

    String downloadUrl = null;
    Instant expiresAt = null;
    if (certificate.getPdfUrl() != null && !certificate.getPdfUrl().isBlank()) {
      var signed =
          r2PresignedUrlService.generateDownloadUrl(
              certificate.getPdfUrl(),
              "Certificate-" + certificate.getTargetSlug() + ".pdf",
              "application/pdf");
      downloadUrl = signed.downloadUrl();
      expiresAt = signed.expiresAt();
    }

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
        .downloadUrl(downloadUrl)
        .downloadUrlExpiresAt(expiresAt)
        .verificationUrl(VERIFICATION_BASE_PATH + certificate.getCertificateCode())
        .wasEligible(eligible)
        .eligibilityReason(eligibilityReason)
        .message("Certificate issued successfully")
        .build();
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

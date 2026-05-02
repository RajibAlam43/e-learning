package com.gii.api.service.certificate;

import com.gii.api.model.response.certificate.PublicCertificateVerificationResponse;
import com.gii.common.entity.certificate.Certificate;
import com.gii.common.entity.course.CourseInstructor;
import com.gii.common.enums.InstructorRole;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.certificate.CertificateRepository;
import com.gii.common.repository.course.CourseInstructorRepository;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.enrollment.LessonProgressRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CertificateVerificationService {

  private final CertificateRepository certificateRepository;
  private final CourseInstructorRepository courseInstructorRepository;
  private final LessonRepository lessonRepository;
  private final LessonProgressRepository lessonProgressRepository;

  public PublicCertificateVerificationResponse execute(String code) {
    String normalizedCode = normalizeCode(code);
    Certificate certificate =
        certificateRepository
            .findByCertificateCode(normalizedCode)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate not found"));

    String instructorName =
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
            .map(instructor -> instructor.getFullName())
            .orElse("Instructor");

    long totalLessons =
        lessonRepository.countByCourseIdAndStatus(
            certificate.getCourse().getId(), PublishStatus.PUBLISHED);
    long completedLessons =
        lessonProgressRepository.countByUserIdAndLessonCourseIdAndCompletedAtIsNotNull(
            certificate.getUser().getId(), certificate.getCourse().getId());
    Double completionPct =
        totalLessons == 0
            ? null
            : Math.round(((completedLessons * 10000.0) / totalLessons)) / 100.0;

    String status = certificate.getRevokedAt() == null ? "VALID" : "REVOKED";
    String message =
        certificate.getRevokedAt() == null
            ? "Certificate is valid."
            : "Certificate has been revoked.";

    return PublicCertificateVerificationResponse.builder()
        .isValid(certificate.getRevokedAt() == null)
        .certificateId(certificate.getId())
        .certificateCode(certificate.getCertificateCode())
        .recipientName(certificate.getRecipientName())
        .courseName(certificate.getCourseTitle())
        .courseSlug(certificate.getCourse().getSlug())
        .instructorName(instructorName)
        .issuedAt(certificate.getIssuedAt())
        .isRevoked(certificate.getRevokedAt() != null)
        .revokedAt(certificate.getRevokedAt())
        .verificationStatus(status)
        .verificationMessage(message)
        .certificateImageUrl(null)
        .issuerName("Global Islamic Institute")
        .issuerLogoUrl(null)
        .completionCriteria("Completed all published lessons")
        .completionPercentage(completionPct)
        .build();
  }

  private String normalizeCode(String code) {
    return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
  }
}

package com.gii.api.service.student;

import com.gii.api.model.response.student.StudentCourseSummaryResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.api.service.localization.LocalizedContentService;
import com.gii.api.service.progress.CourseCompletionService;
import com.gii.api.service.progress.CourseCompletionService.CourseCompletion;
import com.gii.api.service.storage.AssetUrlService;
import com.gii.common.entity.certificate.Certificate;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseInstructor;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.InstructorRole;
import com.gii.common.repository.certificate.CertificateRepository;
import com.gii.common.repository.course.CourseInstructorRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrolledCoursesService {

  private final CurrentUserService currentUserService;
  private final EnrollmentRepository enrollmentRepository;
  private final CourseCompletionService courseCompletionService;
  private final CertificateRepository certificateRepository;
  private final CourseInstructorRepository courseInstructorRepository;
  private final AssetUrlService assetUrlService;
  private final LocalizedContentService localizedContentService;

  public List<StudentCourseSummaryResponse> execute(Authentication authentication) {
    UUID userId = currentUserService.getCurrentUserId(authentication);
    List<Enrollment> enrollments =
        enrollmentRepository.findByUserIdAndStatus(userId, EnrollmentStatus.ACTIVE);
    if (enrollments.isEmpty()) {
      return List.of();
    }

    List<UUID> courseIds = enrollments.stream().map(e -> e.getCourse().getId()).toList();
    Map<UUID, String> instructorNameByCourseId = buildInstructorNameMap(courseIds);
    Map<UUID, CourseCompletion> completionByCourseId =
        courseCompletionService.getByCourseIds(userId, courseIds);
    Map<UUID, Certificate> activeCertificateByCourseId =
        getActiveCertificatesByCourseId(userId, courseIds);

    return enrollments.stream()
        .map(
            enrollment ->
                toCourseSummary(
                    enrollment,
                    instructorNameByCourseId,
                    completionByCourseId,
                    activeCertificateByCourseId))
        .toList();
  }

  private StudentCourseSummaryResponse toCourseSummary(
      Enrollment enrollment,
      Map<UUID, String> instructorNameByCourseId,
      Map<UUID, CourseCompletion> completionByCourseId,
      Map<UUID, Certificate> activeCertificateByCourseId) {
    Course course = enrollment.getCourse();
    CourseCompletion completion = completionByCourseId.get(course.getId());

    Certificate certificate = activeCertificateByCourseId.get(course.getId());

    return StudentCourseSummaryResponse.builder()
        .courseId(course.getId())
        .courseName(localizedContentService.text(course.getTitle(), course.getTitleEn()))
        .courseSlug(course.getSlug())
        .instructorName(instructorNameByCourseId.get(course.getId()))
        .courseThumbnailUrl(assetUrlService.publicUrl(course.getThumbnailObjectKey()))
        .completionPercentage(completion.completionPercentage())
        .completedLessons(completion.completedLessons())
        .totalLessons(completion.totalLessons())
        .completedItems(completion.completedItems())
        .totalItems(completion.totalItems())
        .enrollmentStatus(enrollment.getStatus())
        .enrolledAt(enrollment.getEnrolledAt())
        .completedAt(enrollment.getCompletedAt())
        .expiresAt(enrollment.getExpiresAt())
        .courseLevel(course.getLevel().name())
        .language(course.getLanguage().name())
        .hasCertificate(certificate != null)
        .certificateCode(certificate != null ? certificate.getCertificateCode() : null)
        .build();
  }

  private Map<UUID, Certificate> getActiveCertificatesByCourseId(
      UUID userId, List<UUID> courseIds) {
    Map<UUID, Certificate> result = new HashMap<>();
    for (Certificate certificate :
        certificateRepository.findActiveByUserIdAndCourseIds(userId, courseIds)) {
      result.put(certificate.getCourse().getId(), certificate);
    }
    return result;
  }

  private Map<UUID, String> buildInstructorNameMap(List<UUID> courseIds) {
    List<CourseInstructor> instructors = courseInstructorRepository.findByCourseIds(courseIds);
    Map<UUID, String> map = new HashMap<>();
    for (CourseInstructor instructor : instructors) {
      UUID courseId = instructor.getCourse().getId();
      String fullName = instructor.getInstructor().getFullName();
      if (!map.containsKey(courseId) || instructor.getRole() == InstructorRole.PRIMARY) {
        map.put(courseId, fullName);
      }
    }
    return map;
  }
}

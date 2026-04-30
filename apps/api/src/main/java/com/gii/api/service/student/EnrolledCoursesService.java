package com.gii.api.service.student;

import com.gii.api.model.response.student.StudentCourseSummaryResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.entity.certificate.Certificate;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseInstructor;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.user.User;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.InstructorRole;
import com.gii.common.repository.certificate.CertificateRepository;
import com.gii.common.repository.course.CourseInstructorRepository;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.enrollment.LessonProgressRepository;
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
  private final LessonRepository lessonRepository;
  private final LessonProgressRepository lessonProgressRepository;
  private final CertificateRepository certificateRepository;
  private final CourseInstructorRepository courseInstructorRepository;

  public List<StudentCourseSummaryResponse> execute(Authentication authentication) {
    User user = currentUserService.getCurrentUser(authentication);
    List<Enrollment> enrollments =
        enrollmentRepository.findByUserIdAndStatus(user.getId(), EnrollmentStatus.ACTIVE);
    if (enrollments.isEmpty()) {
      return List.of();
    }

    Map<UUID, String> instructorNameByCourseId =
        buildInstructorNameMap(enrollments.stream().map(e -> e.getCourse().getId()).toList());

    return enrollments.stream()
        .map(enrollment -> toCourseSummary(user.getId(), enrollment, instructorNameByCourseId))
        .toList();
  }

  private StudentCourseSummaryResponse toCourseSummary(
      UUID userId, Enrollment enrollment, Map<UUID, String> instructorNameByCourseId) {
    Course course = enrollment.getCourse();
    long totalLessonsLong =
        lessonRepository.countByCourseIdAndStatus(
            course.getId(), com.gii.common.enums.PublishStatus.PUBLISHED);
    int totalLessons = (int) totalLessonsLong;
    int completedLessons =
        (int)
            lessonProgressRepository.countByUserIdAndLessonCourseIdAndCompletedAtIsNotNull(
                userId, course.getId());
    double completion = totalLessons == 0 ? 0.0 : (completedLessons * 100.0) / totalLessons;

    java.util.Optional<Certificate> certificateOpt =
        certificateRepository
            .findByUserIdAndCourseId(userId, course.getId())
            .filter(certificate -> certificate.getRevokedAt() == null);

    return StudentCourseSummaryResponse.builder()
        .courseId(course.getId())
        .courseName(course.getTitle())
        .courseSlug(course.getSlug())
        .instructorName(instructorNameByCourseId.get(course.getId()))
        .courseThumbnailUrl(course.getThumbnailUrl())
        .completionPercentage(round2(completion))
        .completedLessons(completedLessons)
        .totalLessons(totalLessons)
        .enrollmentStatus(enrollment.getStatus())
        .enrolledAt(enrollment.getEnrolledAt())
        .completedAt(enrollment.getCompletedAt())
        .expiresAt(enrollment.getExpiresAt())
        .courseLevel(course.getLevel().name())
        .language(course.getLanguage().name())
        .hasCertificate(certificateOpt.isPresent())
        .certificateCode(certificateOpt.map(Certificate::getCertificateCode).orElse(null))
        .build();
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

  private double round2(double value) {
    return Math.round(value * 100.0) / 100.0;
  }
}

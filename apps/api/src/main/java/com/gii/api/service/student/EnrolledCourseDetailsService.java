package com.gii.api.service.student;

import com.gii.api.model.response.student.StudentCourseHomeResponse;
import com.gii.api.model.response.student.StudentLessonHomeResponse;
import com.gii.api.model.response.student.StudentSectionHomeResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.entity.certificate.Certificate;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseInstructor;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.enrollment.LessonProgress;
import com.gii.common.entity.user.User;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.InstructorRole;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.certificate.CertificateRepository;
import com.gii.common.repository.course.CourseInstructorRepository;
import com.gii.common.repository.course.CourseSectionRepository;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.enrollment.LessonProgressRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class EnrolledCourseDetailsService {

  private final CurrentUserService currentUserService;
  private final EnrollmentRepository enrollmentRepository;
  private final LessonRepository lessonRepository;
  private final LessonProgressRepository lessonProgressRepository;
  private final CourseSectionRepository courseSectionRepository;
  private final CourseInstructorRepository courseInstructorRepository;
  private final CertificateRepository certificateRepository;

  public StudentCourseHomeResponse execute(UUID courseId, Authentication authentication) {
    User user = currentUserService.getCurrentUser(authentication);
    Enrollment enrollment =
        enrollmentRepository
            .findByUserIdAndCourseId(user.getId(), courseId)
            .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Course not found or not enrolled"));

    Course course = enrollment.getCourse();
    List<CourseSection> sections =
        courseSectionRepository.findByCourseIdAndStatusOrderByPositionAsc(
            courseId, PublishStatus.PUBLISHED);
    List<Lesson> lessons =
        lessonRepository.findByCourseIdAndStatusWithMediaOrderByPositionAsc(
            courseId, PublishStatus.PUBLISHED);
    List<LessonProgress> progresses =
        lessonProgressRepository.findByUserIdAndLessonCourseId(user.getId(), courseId);

    Map<UUID, LessonProgress> progressByLessonId = new HashMap<>();
    for (LessonProgress progress : progresses) {
      progressByLessonId.put(progress.getLesson().getId(), progress);
    }

    String instructorName = resolveInstructorName(courseId);
    int totalLessons = lessons.size();
    int completedLessons =
        (int) progresses.stream().filter(p -> p.getCompletedAt() != null).count();
    double completionPercentage =
        totalLessons == 0 ? 0.0 : (completedLessons * 100.0) / totalLessons;

    Map<UUID, List<Lesson>> lessonsBySectionId =
        lessons.stream()
            .collect(java.util.stream.Collectors.groupingBy(l -> l.getSection().getId()));

    List<StudentSectionHomeResponse> sectionResponses =
        sections.stream()
            .map(
                section ->
                    toSectionHome(
                        section,
                        lessonsBySectionId.getOrDefault(section.getId(), List.of()),
                        progressByLessonId))
            .toList();

    java.util.Optional<Certificate> cert =
        certificateRepository
            .findByUserIdAndCourseId(user.getId(), courseId)
            .filter(c -> c.getRevokedAt() == null);

    return StudentCourseHomeResponse.builder()
        .courseId(course.getId())
        .courseName(course.getTitle())
        .courseSlug(course.getSlug())
        .description(course.getDescription())
        .thumbnailUrl(course.getThumbnailUrl())
        .instructor(instructorName)
        .courseLevel(course.getLevel().name())
        .enrollmentStatus(enrollment.getStatus())
        .enrolledAt(enrollment.getEnrolledAt())
        .expiresAt(enrollment.getExpiresAt())
        .isExpired(
            enrollment.getExpiresAt() != null && enrollment.getExpiresAt().isBefore(Instant.now()))
        .completionPercentage(round2(completionPercentage))
        .completedLessons(completedLessons)
        .totalLessons(totalLessons)
        .sections(sectionResponses)
        .liveSessions(course.getLiveSessionCount())
        .quizzes(course.getQuizCount())
        .estimatedDurationHours(formatDurationHours(course.getEstimatedDurationMinutes()))
        .hasCertificate(cert.isPresent())
        .certificateCode(cert.map(Certificate::getCertificateCode).orElse(null))
        .build();
  }

  private StudentSectionHomeResponse toSectionHome(
      CourseSection section, List<Lesson> lessons, Map<UUID, LessonProgress> progressByLessonId) {
    int totalLessons = lessons.size();
    int completedLessons =
        (int)
            lessons.stream()
                .map(Lesson::getId)
                .map(progressByLessonId::get)
                .filter(p -> p != null && p.getCompletedAt() != null)
                .count();
    double completion = totalLessons == 0 ? 0.0 : (completedLessons * 100.0) / totalLessons;

    List<StudentLessonHomeResponse> lessonResponses = new java.util.ArrayList<>();
    for (int i = 0; i < lessons.size(); i++) {
      Lesson lesson = lessons.get(i);
      LessonProgress progress = progressByLessonId.get(lesson.getId());
      String prev = i > 0 ? lessons.get(i - 1).getId().toString() : null;
      String next = i < lessons.size() - 1 ? lessons.get(i + 1).getId().toString() : null;

      lessonResponses.add(
          StudentLessonHomeResponse.builder()
              .lessonId(lesson.getId())
              .lessonTitle(lesson.getTitle())
              .position(lesson.getPosition())
              .lessonType(lesson.getLessonType())
              .completed(progress != null && progress.getCompletedAt() != null)
              .completedAt(progress != null ? progress.getCompletedAt() : null)
              .lastPositionSec(progress != null ? progress.getLastPositionSec() : null)
              .isAccessible(true)
              .durationLabel(formatLessonDuration(lesson.getDurationSeconds()))
              .isFree(lesson.getIsFree())
              .nextLessonId(next)
              .previousLessonId(prev)
              .build());
    }

    return StudentSectionHomeResponse.builder()
        .sectionId(section.getId())
        .sectionTitle(section.getTitle())
        .position(section.getPosition())
        .description(section.getDescription())
        .completionPercentage(round2(completion))
        .completedLessons(completedLessons)
        .totalLessons(totalLessons)
        .isAccessible(true)
        .accessReason("AVAILABLE")
        .lessons(lessonResponses)
        .build();
  }

  private String resolveInstructorName(UUID courseId) {
    List<CourseInstructor> instructors = courseInstructorRepository.findByCourseId(courseId);
    return instructors.stream()
        .filter(i -> i.getRole() == InstructorRole.PRIMARY)
        .findFirst()
        .or(() -> instructors.stream().findFirst())
        .map(i -> i.getInstructor().getFullName())
        .orElse("Instructor");
  }

  private String formatLessonDuration(Integer durationSec) {
    if (durationSec == null || durationSec <= 0) {
      return null;
    }
    long minutes = Math.max(1, Duration.ofSeconds(durationSec).toMinutes());
    return minutes + " minutes";
  }

  private String formatDurationHours(Integer minutes) {
    if (minutes == null || minutes <= 0) {
      return null;
    }
    double hours = minutes / 60.0;
    return round2(hours) + " hours";
  }

  private double round2(double value) {
    return Math.round(value * 100.0) / 100.0;
  }
}

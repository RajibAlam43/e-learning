package com.gii.api.service.lesson;

import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.api.service.enrollment.CurriculumAccessService;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.user.User;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
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
@Transactional(readOnly = true)
public class LessonAccessService {

  private final CurrentUserService currentUserService;
  private final LessonRepository lessonRepository;
  private final CourseRepository courseRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final CurriculumAccessService curriculumAccessService;

  public UUID requireCurrentUserId(Authentication authentication) {
    return currentUserService.getCurrentUserId(authentication);
  }

  public User requireCurrentUser(Authentication authentication) {
    return currentUserService.getCurrentUser(authentication);
  }

  public Lesson requirePublishedLesson(java.util.UUID lessonId) {
    Lesson lesson =
        lessonRepository
            .findById(lessonId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
    if (lesson.getStatus() != PublishStatus.PUBLISHED
        || lesson.getSection().getStatus() != PublishStatus.PUBLISHED) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found");
    }
    return lesson;
  }

  public Enrollment requireActiveEnrollment(UUID userId, Lesson lesson) {
    var enrollments =
        enrollmentRepository
            .findByUserIdAndTemplateVersionIdAndStatus(
                userId, lesson.getSection().getTemplateVersion().getId(), EnrollmentStatus.ACTIVE)
            .stream()
            .filter(e -> !curriculumAccessService.isEnrollmentExpired(e, Instant.now()))
            .toList();
    if (enrollments.size() > 1) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Multiple active courses contain this lesson; use the course-scoped endpoint");
    }
    return enrollments.stream()
        .findFirst()
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You do not have access to this lesson"));
  }

  public Enrollment requireActiveEnrollment(UUID userId, UUID courseId, Lesson lesson) {
    requireLessonInCourse(courseId, lesson);
    Enrollment enrollment =
        enrollmentRepository
            .findByUserIdAndCourseIdAndStatus(userId, courseId, EnrollmentStatus.ACTIVE)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "You do not have access to this lesson"));
    curriculumAccessService.requireSectionAccess(
        lesson.getSection(), enrollment, Instant.now(), "Lesson");
    return enrollment;
  }

  public void requireLessonInCourse(UUID courseId, Lesson lesson) {
    var course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    if (!course
        .getTemplateVersion()
        .getId()
        .equals(lesson.getSection().getTemplateVersion().getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found in course");
    }
  }

  public boolean isLessonAccessible(Lesson lesson, Enrollment enrollment, Instant now) {
    if (!curriculumAccessService.isSectionAccessible(lesson.getSection(), enrollment, now)) {
      return false;
    }

    if (Boolean.TRUE.equals(lesson.getIsFree())) {
      return true;
    }

    return curriculumAccessService.isReleased(
        lesson.getReleaseType(),
        lesson.getReleaseAt(),
        lesson.getUnlockAfterDays(),
        enrollment,
        now);
  }
}

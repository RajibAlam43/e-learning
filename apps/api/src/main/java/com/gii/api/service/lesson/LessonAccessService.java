package com.gii.api.service.lesson;

import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.user.User;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.ReleaseType;
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
  private final EnrollmentRepository enrollmentRepository;

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
    if (lesson.getStatus() != PublishStatus.PUBLISHED) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found");
    }
    return lesson;
  }

  public Enrollment requireActiveEnrollment(UUID userId, Lesson lesson) {
    return enrollmentRepository
        .findByUserIdAndCourseId(userId, lesson.getCourse().getId())
        .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVE)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You do not have access to this lesson"));
  }

  public boolean isLessonAccessible(Lesson lesson, Enrollment enrollment, Instant now) {
    if (Boolean.TRUE.equals(lesson.getIsFree())) {
      return true;
    }

    if (enrollment.getExpiresAt() != null && enrollment.getExpiresAt().isBefore(now)) {
      return false;
    }

    ReleaseType releaseType = lesson.getReleaseType();
    if (releaseType == null || releaseType == ReleaseType.IMMEDIATE) {
      return true;
    }

    if (releaseType == ReleaseType.FIXED_DATE) {
      return lesson.getReleaseAt() == null || !now.isBefore(lesson.getReleaseAt());
    }

    if (releaseType == ReleaseType.RELATIVE_DAYS) {
      if (lesson.getUnlockAfterDays() == null) {
        return true;
      }
      Instant unlockAt =
          enrollment.getEnrolledAt().plusSeconds(lesson.getUnlockAfterDays() * 24L * 3600L);
      return !now.isBefore(unlockAt);
    }

    return true;
  }
}

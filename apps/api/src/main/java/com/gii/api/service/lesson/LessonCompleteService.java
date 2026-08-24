package com.gii.api.service.lesson;

import com.gii.api.service.progress.EnrollmentCompletionService;
import com.gii.api.service.student.StudentLearningStreakTrackerService;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.enrollment.LessonProgress;
import com.gii.common.entity.enrollment.LessonProgressId;
import com.gii.common.entity.user.User;
import com.gii.common.repository.enrollment.LessonProgressRepository;
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
@Transactional
public class LessonCompleteService {

  private final LessonAccessService lessonAccessService;
  private final LessonProgressRepository lessonProgressRepository;
  private final StudentLearningStreakTrackerService studentLearningStreakTrackerService;
  private final EnrollmentCompletionService enrollmentCompletionService;

  public void execute(UUID lessonId, Authentication authentication) {
    User user = lessonAccessService.requireCurrentUser(authentication);
    Lesson lesson = lessonAccessService.requirePublishedLesson(lessonId);
    Enrollment enrollment = lessonAccessService.requireActiveEnrollment(user.getId(), lesson);
    complete(user, lesson, enrollment);
  }

  public void execute(UUID courseId, UUID lessonId, Authentication authentication) {
    User user = lessonAccessService.requireCurrentUser(authentication);
    Lesson lesson = lessonAccessService.requirePublishedLesson(lessonId);
    Enrollment enrollment =
        lessonAccessService.requireActiveEnrollment(user.getId(), courseId, lesson);
    complete(user, lesson, enrollment);
  }

  private void complete(User user, Lesson lesson, Enrollment enrollment) {
    if (!lessonAccessService.isLessonAccessible(lesson, enrollment, Instant.now())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Lesson is not available yet");
    }

    LessonProgressId id =
        LessonProgressId.builder()
            .enrollmentId(enrollment.getId())
            .lessonId(lesson.getId())
            .build();
    LessonProgress progress =
        lessonProgressRepository
            .findById(id)
            .orElse(
                LessonProgress.builder()
                    .id(id)
                    .enrollment(enrollment)
                    .lesson(lesson)
                    .updatedAt(Instant.now())
                    .build());
    if (progress.getCompletedAt() != null) {
      return;
    }
    Instant completedAt = Instant.now();
    progress.setCompletedAt(completedAt);
    lessonProgressRepository.save(progress);
    studentLearningStreakTrackerService.recordActivity(user.getId(), completedAt);
    enrollmentCompletionService.refresh(user.getId(), enrollment.getCourse().getId());
  }
}

package com.gii.api.service.lesson;

import com.gii.api.model.request.lesson.SaveLessonProgressRequest;
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
public class LessonProgressService {

  private final LessonAccessService lessonAccessService;
  private final LessonProgressRepository lessonProgressRepository;

  public void execute(
      UUID lessonId, SaveLessonProgressRequest request, Authentication authentication) {
    User user = lessonAccessService.requireCurrentUser(authentication);
    Lesson lesson = lessonAccessService.requirePublishedLesson(lessonId);
    Enrollment enrollment = lessonAccessService.requireActiveEnrollment(user.getId(), lesson);

    if (!lessonAccessService.isLessonAccessible(lesson, enrollment, Instant.now())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Lesson is not available yet");
    }

    LessonProgressId progressId =
        LessonProgressId.builder().userId(user.getId()).lessonId(lessonId).build();

    LessonProgress progress =
        lessonProgressRepository
            .findById(progressId)
            .orElse(
                LessonProgress.builder()
                    .id(progressId)
                    .user(user)
                    .lesson(lesson)
                    .updatedAt(Instant.now())
                    .lastPositionSec(0)
                    .build());

    if (request.lastPositionSec() != null) {
      progress.setLastPositionSec(request.lastPositionSec());
    }

    if (Boolean.TRUE.equals(request.completed())) {
      progress.setCompletedAt(Instant.now());
    }

    lessonProgressRepository.save(progress);
  }
}

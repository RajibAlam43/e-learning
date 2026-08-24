package com.gii.api.service.media;

import com.gii.api.model.response.MediaPlaybackResponse;
import com.gii.api.service.lesson.LessonAccessService;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.course.MediaAsset;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.enums.MediaStatus;
import com.gii.common.repository.course.MediaAssetRepository;
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
public class MediaPlaybackService {

  private final MediaAssetRepository mediaAssetRepository;
  private final LessonAccessService lessonAccessService;
  private final MediaPlaybackRouter mediaPlaybackRouter;

  public MediaPlaybackResponse getLessonPlayback(UUID lessonId, Authentication authentication) {
    Lesson lesson = lessonAccessService.requirePublishedLesson(lessonId);
    if (Boolean.TRUE.equals(lesson.getIsFree())) {
      return getPlayback(lesson, null);
    }
    UUID userId = lessonAccessService.requireCurrentUserId(authentication);
    Enrollment enrollment = lessonAccessService.requireActiveEnrollment(userId, lesson);
    return getPlayback(lesson, enrollment);
  }

  public MediaPlaybackResponse getLessonPlayback(
      UUID courseId, UUID lessonId, Authentication authentication) {
    Lesson lesson = lessonAccessService.requirePublishedLesson(lessonId);
    if (Boolean.TRUE.equals(lesson.getIsFree())) {
      lessonAccessService.requireLessonInCourse(courseId, lesson);
      return getPlayback(lesson, null);
    }
    UUID userId = lessonAccessService.requireCurrentUserId(authentication);
    Enrollment enrollment = lessonAccessService.requireActiveEnrollment(userId, courseId, lesson);
    return getPlayback(lesson, enrollment);
  }

  private MediaPlaybackResponse getPlayback(Lesson lesson, Enrollment enrollment) {
    MediaAsset mediaAsset =
        mediaAssetRepository
            .findByLessonId(lesson.getId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Media asset not found for lesson"));

    if (mediaAsset.getStatus() != MediaStatus.READY) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media is not ready");
    }

    if (enrollment != null
        && !lessonAccessService.isLessonAccessible(lesson, enrollment, Instant.now())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Lesson is not available yet");
    }

    return mediaPlaybackRouter.getPlayback(mediaAsset);
  }
}

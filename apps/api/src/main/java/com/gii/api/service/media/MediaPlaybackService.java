package com.gii.api.service.media;

import com.gii.api.model.response.MediaPlaybackResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.api.service.enrollment.EnrollmentAccessService;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.course.MediaAsset;
import com.gii.common.enums.MediaStatus;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.course.MediaAssetRepository;
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

  private final LessonRepository lessonRepository;
  private final MediaAssetRepository mediaAssetRepository;
  private final EnrollmentAccessService enrollmentAccessService;
  private final MediaPlaybackRouter mediaPlaybackRouter;
  private final CurrentUserService currentUserService;

  public MediaPlaybackResponse getLessonPlayback(UUID lessonId, Authentication authentication) {
    Lesson lesson =
        lessonRepository
            .findById(lessonId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));

    MediaAsset mediaAsset =
        mediaAssetRepository
            .findByLessonId(lessonId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Media asset not found for lesson"));

    if (mediaAsset.getStatus() != MediaStatus.READY) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media is not ready");
    }

    if (!lesson.getIsFree()) {
      UUID userId = currentUserService.getCurrentUserId(authentication);
      enrollmentAccessService.verifyCanAccessLesson(userId, lessonId);
    }

    return mediaPlaybackRouter.getPlayback(mediaAsset);
  }
}

package com.gii.api.service.media;

import com.gii.api.service.enrollment.EnrollmentAccessService;
import com.gii.api.model.response.MediaPlaybackResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.course.MediaAsset;
import com.gii.common.entity.user.User;
import com.gii.common.enums.MediaStatus;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.course.MediaAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        MediaAsset mediaAsset = mediaAssetRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new RuntimeException("Media asset not found for lesson"));

        if (mediaAsset.getStatus() != MediaStatus.READY) {
            throw new RuntimeException("Media is not ready");
        }

        if (!lesson.getIsFree()) {
            User user = currentUserService.getCurrentUser(authentication);

            enrollmentAccessService.verifyCanAccessLesson(
                    user.getId(),
                    lessonId
            );
        }

        return mediaPlaybackRouter.getPlayback(mediaAsset);
    }
}

package com.gii.api.service.student;

import com.gii.api.model.response.LessonPlaybackResponse;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.course.MediaAsset;
import com.gii.common.entity.user.User;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.MediaProvider;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonPlaybackService {

    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final MuxTokenService muxTokenService;

    public LessonPlaybackResponse execute(UUID lessonId, Authentication authentication) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        MediaAsset media = lesson.getMediaAsset();

        if (media == null) {
            throw new RuntimeException("No media attached");
        }

        // Free preview allowed
        if (Boolean.TRUE.equals(lesson.getIsPreviewFree())) {
            return LessonPlaybackResponse.builder()
                    .lessonId(lesson.getId())
                    .provider(media.getProvider())
                    .sourceId(media.getProviderAssetId())
                    .build();
        }

        // Paid lesson requires login
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Authentication required");
        }

        User user = (User) authentication.getPrincipal();

        boolean enrolled = enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                user.getId(),
                lesson.getCourse().getId(),
                EnrollmentStatus.active
        );

        if (!enrolled) {
            throw new RuntimeException("You are not enrolled in this course");
        }

        // Paid content must be Mux
        if (media.getProvider() != MediaProvider.mux) {
            throw new RuntimeException("Invalid paid media provider");
        }

        String muxToken = muxTokenService.generatePlaybackToken(media.getPlaybackId());

        return LessonPlaybackResponse.builder()
                .lessonId(lesson.getId())
                .provider(MediaProvider.mux)
                .playbackId(media.getPlaybackId())
                .muxToken(muxToken)
                .build();
    }
}

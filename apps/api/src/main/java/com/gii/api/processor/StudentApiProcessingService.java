package com.gii.api.processor;

import com.gii.api.model.response.MediaPlaybackResponse;
import com.gii.api.service.media.MediaPlaybackService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentApiProcessingService {

    private final MediaPlaybackService mediaPlaybackService;

    public MediaPlaybackResponse getLessonPlayback(UUID lessonId, Authentication authentication) {
        return mediaPlaybackService.getLessonPlayback(lessonId, authentication);
    }
}

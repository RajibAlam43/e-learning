package com.gii.api.service.lesson;

import com.gii.api.model.response.MediaPlaybackResponse;
import com.gii.api.service.media.MediaPlaybackService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonPlaybackService {

  private final MediaPlaybackService mediaPlaybackService;

  public MediaPlaybackResponse execute(UUID lessonId, Authentication authentication) {
    return mediaPlaybackService.getLessonPlayback(lessonId, authentication);
  }
}

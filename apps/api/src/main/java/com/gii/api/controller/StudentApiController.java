package com.gii.api.controller;

import com.gii.api.model.response.LessonPlaybackResponse;
import com.gii.api.model.response.MediaPlaybackResponse;
import com.gii.api.processor.StudentApiProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/student")
public class StudentApiController {

    private final StudentApiProcessingService studentApiProcessingService;

    @PostMapping("/lessons/{lessonId}/playback")
    public ResponseEntity<MediaPlaybackResponse> getPlayback(
            @PathVariable UUID lessonId,
            Authentication authentication
    ) {
        MediaPlaybackResponse response =
                studentApiProcessingService.getLessonPlayback(lessonId, authentication);

        return ResponseEntity.ok(response);
    }
}

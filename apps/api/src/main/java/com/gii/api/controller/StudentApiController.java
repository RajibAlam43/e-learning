package com.gii.api.controller;

import com.gii.api.model.response.LessonPlaybackResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/student")
public class StudentApiController {

    @GetMapping("/lessons/{lessonId}/playback")
    public ResponseEntity<LessonPlaybackResponse> getPlayback(
            @PathVariable UUID lessonId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                lessonPlaybackService.execute(lessonId, authentication)
        );
    }
}

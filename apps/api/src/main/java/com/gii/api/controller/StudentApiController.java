package com.gii.api.controller;

import com.gii.api.model.response.LessonPlaybackResponse;
import com.gii.api.processor.StudentApiProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/student")
public class StudentApiController {

    private final StudentApiProcessingService studentApiProcessingService;

    @GetMapping("/lessons/{lessonId}/playback")
    public ResponseEntity<LessonPlaybackResponse> getPlayback(
            @PathVariable UUID lessonId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
        //        studentApiProcessingService.execute(lessonId, authentication)
                null
        );
    }
}

package com.gii.api.controller;

import com.gii.api.model.request.lesson.SaveLessonProgressRequest;
import com.gii.api.model.response.MediaPlaybackResponse;
import com.gii.api.model.response.lesson.CourseProgressResponse;
import com.gii.api.model.response.lesson.LessonContentResponse;
import com.gii.api.model.response.lesson.LessonResourceResponse;
import com.gii.api.model.response.lesson.ResourceDownloadUrlResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class LessonApiController implements  LessonApi {

    @Override
    public ResponseEntity<LessonContentResponse> getLessonContent(UUID lessonId, Authentication authentication) {
        return null;
    }

    @Override
    public ResponseEntity<MediaPlaybackResponse> getLessonPlayback(UUID lessonId, Authentication authentication) {
        return null;
    }

    @Override
    public ResponseEntity<Void> saveLessonProgress(UUID lessonId, SaveLessonProgressRequest request, Authentication authentication) {
        return null;
    }

    @Override
    public ResponseEntity<Void> markLessonComplete(UUID lessonId, Authentication authentication) {
        return null;
    }

    @Override
    public ResponseEntity<CourseProgressResponse> getCourseProgress(UUID courseId, Authentication authentication) {
        return null;
    }

    @Override
    public ResponseEntity<List<LessonResourceResponse>> getLessonResources(UUID lessonId, Authentication authentication) {
        return null;
    }

    @Override
    public ResponseEntity<ResourceDownloadUrlResponse> getResourceDownloadUrl(UUID resourceId, Authentication authentication) {
        return null;
    }
}

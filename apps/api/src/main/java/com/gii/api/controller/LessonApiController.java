package com.gii.api.controller;

import com.gii.api.model.request.lesson.SaveLessonProgressRequest;
import com.gii.api.model.response.MediaPlaybackResponse;
import com.gii.api.model.response.lesson.CourseProgressResponse;
import com.gii.api.model.response.lesson.LessonContentResponse;
import com.gii.api.model.response.lesson.LessonResourceResponse;
import com.gii.api.model.response.lesson.ResourceDownloadUrlResponse;
import com.gii.api.service.lesson.CourseProgressService;
import com.gii.api.service.lesson.LessonCompleteService;
import com.gii.api.service.lesson.LessonContentService;
import com.gii.api.service.lesson.LessonPlaybackService;
import com.gii.api.service.lesson.LessonProgressService;
import com.gii.api.service.lesson.LessonResourcesService;
import com.gii.api.service.lesson.ResourceDownloadService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LessonApiController implements LessonApi {

  private final LessonContentService lessonContentService;
  private final LessonPlaybackService lessonPlaybackService;
  private final LessonProgressService lessonProgressService;
  private final LessonCompleteService lessonCompleteService;
  private final CourseProgressService courseProgressService;
  private final LessonResourcesService lessonResourcesService;
  private final ResourceDownloadService resourceDownloadService;

  @Override
  public ResponseEntity<LessonContentResponse> getLessonContent(
      UUID lessonId, Authentication authentication) {
    return ResponseEntity.ok(lessonContentService.execute(lessonId, authentication));
  }

  @Override
  public ResponseEntity<MediaPlaybackResponse> getLessonPlayback(
      UUID lessonId, Authentication authentication) {
    return ResponseEntity.ok(lessonPlaybackService.execute(lessonId, authentication));
  }

  @Override
  public ResponseEntity<Void> saveLessonProgress(
      UUID lessonId, SaveLessonProgressRequest request, Authentication authentication) {
    lessonProgressService.execute(lessonId, request, authentication);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> markLessonComplete(UUID lessonId, Authentication authentication) {
    lessonCompleteService.execute(lessonId, authentication);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<CourseProgressResponse> getCourseProgress(
      UUID courseId, Authentication authentication) {
    return ResponseEntity.ok(courseProgressService.execute(courseId, authentication));
  }

  @Override
  public ResponseEntity<List<LessonResourceResponse>> getLessonResources(
      UUID lessonId, Authentication authentication) {
    return ResponseEntity.ok(lessonResourcesService.execute(lessonId, authentication));
  }

  @Override
  public ResponseEntity<ResourceDownloadUrlResponse> getResourceDownloadUrl(
      UUID resourceId, Authentication authentication) {
    return ResponseEntity.ok(resourceDownloadService.execute(resourceId, authentication));
  }
}

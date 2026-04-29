package com.gii.api.controller;

import com.gii.api.model.request.SaveLessonProgressRequest;
import com.gii.api.model.response.CourseProgressResponse;
import com.gii.api.model.response.LessonContentResponse;
import com.gii.api.model.response.MediaPlaybackResponse;
import com.gii.api.processor.LessonApiProcessingService;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/learn")
public class LessonApiController {

    private final LessonApiProcessingService lessonApiProcessingService;

    /**
     * Returns lesson content for the current student, including notes, resources,
     * and access metadata for rendering the lesson page.
     *
     * @param lessonId ID of the lesson to load
     * @param authentication authenticated user context from Spring Security
     * @return lesson content payload with access information
     */
    @GetMapping("/lessons/{lessonId}")
    public ResponseEntity<@NotNull LessonContentResponse> getLessonContent(
            @PathVariable UUID lessonId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(lessonApiProcessingService.getLessonContent(lessonId, authentication));
    }

    /**
     * Returns playback information (for example, YouTube or Mux) after validating
     * that the current student has access to the lesson.
     *
     * @param lessonId ID of the lesson to play
     * @param authentication authenticated user context from Spring Security
     * @return playback payload required by the client player
     */
    @GetMapping("/lessons/{lessonId}/playback")
    public ResponseEntity<@NotNull MediaPlaybackResponse> getLessonPlayback(
            @PathVariable UUID lessonId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(lessonApiProcessingService.getLessonPlayback(lessonId, authentication));
    }

    /**
     * Saves student lesson progress such as completion state and last watched
     * position, if provided by the client.
     *
     * @param lessonId ID of the lesson being tracked
     * @param request lesson progress payload from client
     * @param authentication authenticated user context from Spring Security
     * @return empty success response when progress is stored
     */
    @PostMapping("/lessons/{lessonId}/progress")
    public ResponseEntity<@NotNull Void> saveLessonProgress(
            @PathVariable UUID lessonId,
            @RequestBody SaveLessonProgressRequest request,
            Authentication authentication
    ) {
        lessonApiProcessingService.saveLessonProgress(lessonId, request, authentication);
        return ResponseEntity.ok().build();
    }

    /**
     * Marks a lesson as completed for the current student.
     *
     * @param lessonId ID of the lesson to mark complete
     * @param authentication authenticated user context from Spring Security
     * @return empty success response when completion is recorded
     */
    @PostMapping("/lessons/{lessonId}/complete")
    public ResponseEntity<@NotNull Void> markLessonComplete(
            @PathVariable UUID lessonId,
            Authentication authentication
    ) {
        lessonApiProcessingService.markLessonComplete(lessonId, authentication);
        return ResponseEntity.ok().build();
    }

    /**
     * Returns progress summary for a course for the current student, such as
     * overall completion percentage and related counters.
     *
     * @param courseId ID of the course to evaluate progress for
     * @param authentication authenticated user context from Spring Security
     * @return course progress summary payload
     */
    @GetMapping("/courses/{courseId}/progress")
    public ResponseEntity<@NotNull CourseProgressResponse> getCourseProgress(
            @PathVariable UUID courseId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(lessonApiProcessingService.getCourseProgress(courseId, authentication));
    }

    /**
     * Lists all resources attached to a lesson (for example PDF, image, or file)
     * after validating that the current user can access the lesson.
     *
     * @param lessonId ID of the lesson whose resources are requested
     * @param authentication authenticated user context from Spring Security
     * @return list of lesson resource metadata visible to the current user
     */
    @GetMapping("/lessons/{lessonId}/resources")
    public ResponseEntity<@NotNull List<LessonResourceResponse>> getLessonResources(
            @PathVariable UUID lessonId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(lessonApiProcessingService.getLessonResources(lessonId, authentication));
    }

    /**
     * Returns a signed temporary download URL for a lesson resource after access
     * checks pass for the current user.
     *
     * @param resourceId ID of the resource to download
     * @param authentication authenticated user context from Spring Security
     * @return signed URL payload used by the client to download the resource
     */
    @GetMapping("/resources/{resourceId}/download-url")
    public ResponseEntity<@NotNull ResourceDownloadUrlResponse> getResourceDownloadUrl(
            @PathVariable UUID resourceId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(lessonApiProcessingService.getResourceDownloadUrl(resourceId, authentication));
    }
}

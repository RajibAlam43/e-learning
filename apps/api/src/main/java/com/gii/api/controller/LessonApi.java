package com.gii.api.controller;

import com.gii.api.model.request.lesson.SaveLessonProgressRequest;
import com.gii.api.model.response.*;
import com.gii.api.model.response.lesson.CourseProgressResponse;
import com.gii.api.model.response.lesson.LessonContentResponse;
import com.gii.api.model.response.lesson.LessonResourceResponse;
import com.gii.api.model.response.lesson.ResourceDownloadUrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Lessons", description = "Lesson content, playback, and progress tracking for enrolled students")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/learn")
public interface LessonApi {

    @GetMapping("/lessons/{lessonId}")
    @Operation(
            summary = "Get lesson content",
            description = "Fetch complete lesson content including sections, resources, and access metadata. Validates student enrollment/access."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lesson content retrieved", content = @Content(schema = @Schema(implementation = LessonContentResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied - not enrolled or lesson locked"),
            @ApiResponse(responseCode = "404", description = "Lesson not found")
    })
    ResponseEntity<LessonContentResponse> getLessonContent(
            @PathVariable UUID lessonId,
            Authentication authentication
    );

    @GetMapping("/lessons/{lessonId}/playback")
    @Operation(
            summary = "Get media playback information",
            description = "Get playback URLs and configuration (Mux, YouTube, Bunny) for video/media lessons after access validation."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Playback info retrieved", content = @Content(schema = @Schema(implementation = MediaPlaybackResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Media not found")
    })
    ResponseEntity<MediaPlaybackResponse> getLessonPlayback(
            @PathVariable UUID lessonId,
            Authentication authentication
    );

    @PostMapping("/lessons/{lessonId}/progress")
    @Operation(
            summary = "Save lesson progress",
            description = "Save student progress: completion state, last watched position (for videos)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Progress saved"),
            @ApiResponse(responseCode = "400", description = "Invalid progress data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Lesson not found")
    })
    ResponseEntity<Void> saveLessonProgress(
            @PathVariable UUID lessonId,
            @RequestBody SaveLessonProgressRequest request,
            Authentication authentication
    );

    @PostMapping("/lessons/{lessonId}/complete")
    @Operation(
            summary = "Mark lesson as completed",
            description = "Explicitly mark a lesson as completed by the student."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lesson marked complete"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Lesson not found")
    })
    ResponseEntity<Void> markLessonComplete(
            @PathVariable UUID lessonId,
            Authentication authentication
    );

    @GetMapping("/courses/{courseId}/progress")
    @Operation(
            summary = "Get course progress",
            description = "Get overall course progress: completion percentage, completed/total lessons per section."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Course progress retrieved", content = @Content(schema = @Schema(implementation = CourseProgressResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Course not found or not enrolled")
    })
    ResponseEntity<CourseProgressResponse> getCourseProgress(
            @PathVariable UUID courseId,
            Authentication authentication
    );

    @GetMapping("/lessons/{lessonId}/resources")
    @Operation(
            summary = "List lesson resources",
            description = "Get all downloadable resources (PDFs, images) attached to a lesson after access validation."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resources retrieved", content = @Content(schema = @Schema(implementation = LessonResourceResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Lesson not found")
    })
    ResponseEntity<List<LessonResourceResponse>> getLessonResources(
            @PathVariable UUID lessonId,
            Authentication authentication
    );

    @GetMapping("/resources/{resourceId}/download-url")
    @Operation(
            summary = "Get resource download URL",
            description = "Get signed temporary download URL for a lesson resource after access checks."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Download URL retrieved", content = @Content(schema = @Schema(implementation = ResourceDownloadUrlResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    ResponseEntity<ResourceDownloadUrlResponse> getResourceDownloadUrl(
            @PathVariable UUID resourceId,
            Authentication authentication
    );
}
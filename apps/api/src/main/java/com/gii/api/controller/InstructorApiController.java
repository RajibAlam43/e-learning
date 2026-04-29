package com.gii.api.controller;

import com.gii.api.model.request.CreateLiveClassRequest;
import com.gii.api.model.request.UpdateLiveClassRequest;
import com.gii.api.model.response.InstructorDashboardResponse;
import com.gii.api.model.response.InstructorLiveClassResponse;
import com.gii.api.model.response.InstructorLiveClassStartResponse;
import com.gii.api.processor.InstructorApiProcessingService;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/instructor")
public class InstructorApiController {

    private final InstructorApiProcessingService instructorApiProcessingService;

    /**
     * Returns instructor dashboard data, including assigned courses,
     * per-course enrolled student counts, and upcoming live classes.
     *
     * @param authentication authenticated user context from Spring Security
     * @return instructor dashboard payload
     */
    @GetMapping("/dashboard")
    public ResponseEntity<@NotNull InstructorDashboardResponse> getDashboard(
            Authentication authentication
    ) {
        return ResponseEntity.ok(instructorApiProcessingService.getDashboard(authentication));
    }

    /**
     * Creates/schedules a live class for an instructor-assigned course.
     *
     * @param courseId ID of the course
     * @param request live class creation payload
     * @param authentication authenticated user context from Spring Security
     * @return created live class payload
     */
    @PostMapping("/courses/{courseId}/live-classes")
    public ResponseEntity<@NotNull InstructorLiveClassResponse> createLiveClass(
            @PathVariable UUID courseId,
            @RequestBody CreateLiveClassRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                instructorApiProcessingService.createLiveClass(courseId, request, authentication)
        );
    }

    /**
     * Starts a live class and returns host/start URL details.
     *
     * @param liveClassId ID of the live class
     * @param authentication authenticated user context from Spring Security
     * @return start payload including host/start URL
     */
    @PostMapping("/live-classes/{liveClassId}/start")
    public ResponseEntity<@NotNull InstructorLiveClassStartResponse> startLiveClass(
            @PathVariable UUID liveClassId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                instructorApiProcessingService.startLiveClass(liveClassId, authentication)
        );
    }

    /**
     * Updates live class metadata such as title, schedule, or status.
     *
     * @param liveClassId ID of the live class
     * @param request live class update payload
     * @param authentication authenticated user context from Spring Security
     * @return updated live class payload
     */
    @PatchMapping("/live-classes/{liveClassId}")
    public ResponseEntity<@NotNull InstructorLiveClassResponse> updateLiveClass(
            @PathVariable UUID liveClassId,
            @RequestBody UpdateLiveClassRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                instructorApiProcessingService.updateLiveClass(liveClassId, request, authentication)
        );
    }

    /**
     * Cancels/deletes a live class for an instructor.
     *
     * @param liveClassId ID of the live class
     * @param authentication authenticated user context from Spring Security
     * @return empty success response when the class is removed
     */
    @DeleteMapping("/live-classes/{liveClassId}")
    public ResponseEntity<@NotNull Void> deleteLiveClass(
            @PathVariable UUID liveClassId,
            Authentication authentication
    ) {
        instructorApiProcessingService.deleteLiveClass(liveClassId, authentication);
        return ResponseEntity.ok().build();
    }
}

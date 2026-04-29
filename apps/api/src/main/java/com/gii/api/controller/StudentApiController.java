package com.gii.api.controller;

import com.gii.api.model.response.LessonPlaybackResponse;
import com.gii.api.model.response.MediaPlaybackResponse;
import com.gii.api.processor.StudentApiProcessingService;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/student")
public class StudentApiController {

    private final StudentApiProcessingService studentApiProcessingService;

    /**
     * Returns the current student's dashboard data, including enrolled courses,
     * overall learning progress, and earned certificates overview.
     *
     * @param authentication authenticated user context from Spring Security
     * @return dashboard payload for the logged-in student
     */
    @GetMapping("/dashboard")
    public ResponseEntity<@NotNull StudentDashboardResponse> getDashboard(Authentication authentication) {
        return ResponseEntity.ok(studentApiProcessingService.getDashboard(authentication));
    }

    /**
     * Lists all courses the current student has enrolled in or purchased.
     *
     * @param authentication authenticated user context from Spring Security
     * @return list of enrolled/purchased course summaries
     */
    @GetMapping("/courses")
    public ResponseEntity<@NotNull List<StudentCourseSummaryResponse>> getMyCourses(Authentication authentication) {
        return ResponseEntity.ok(studentApiProcessingService.getMyCourses(authentication));
    }

    /**
     * Returns enrolled course home data for the current student, including
     * sections, lessons, and per-course progress details.
     *
     * @param courseId ID of the enrolled course to fetch
     * @param authentication authenticated user context from Spring Security
     * @return course home payload with learning structure and progress
     */
    @GetMapping("/courses/{courseId}")
    public ResponseEntity<@NotNull StudentCourseHomeResponse> getMyCourseDetails(
            @PathVariable UUID courseId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(studentApiProcessingService.getMyCourseDetails(courseId, authentication));
    }

    /**
     * Lists purchase/order history for the current student.
     *
     * @param authentication authenticated user context from Spring Security
     * @return list of student order summaries
     */
    @GetMapping("/orders")
    public ResponseEntity<@NotNull List<StudentOrderSummaryResponse>> getMyOrders(Authentication authentication) {
        return ResponseEntity.ok(studentApiProcessingService.getMyOrders(authentication));
    }

    /**
     * Lists certificates earned by the current student.
     *
     * @param authentication authenticated user context from Spring Security
     * @return list of earned certificate summaries
     */
    @GetMapping("/certificates")
    public ResponseEntity<@NotNull List<StudentCertificateSummaryResponse>> getMyCertificates(
            Authentication authentication
    ) {
        return ResponseEntity.ok(studentApiProcessingService.getMyCertificates(authentication));
    }

    /**
     * Lists upcoming live classes across all courses the current student is enrolled in.
     *
     * @param authentication authenticated user context from Spring Security
     * @return list of upcoming live classes for the student
     */
    @GetMapping("/live-classes")
    public ResponseEntity<@NotNull List<StudentLiveClassSummaryResponse>> getUpcomingLiveClasses(
            Authentication authentication
    ) {
        return ResponseEntity.ok(studentApiProcessingService.getUpcomingLiveClasses(authentication));
    }

    /**
     * Lists upcoming live classes for a specific enrolled course.
     *
     * @param courseId ID of the enrolled course
     * @param authentication authenticated user context from Spring Security
     * @return list of live classes for the given course
     */
    @GetMapping("/courses/{courseId}/live-classes")
    public ResponseEntity<@NotNull List<StudentLiveClassSummaryResponse>> getCourseLiveClasses(
            @PathVariable UUID courseId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(studentApiProcessingService.getCourseLiveClasses(courseId, authentication));
    }

    /**
     * Checks enrollment and returns a join URL (or join payload) for a live class.
     *
     * @param liveClassId ID of the live class to join
     * @param authentication authenticated user context from Spring Security
     * @return join information for the live class
     */
    @PostMapping("/live-classes/{liveClassId}/join")
    public ResponseEntity<@NotNull StudentLiveClassJoinResponse> joinLiveClass(
            @PathVariable UUID liveClassId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(studentApiProcessingService.joinLiveClass(liveClassId, authentication));
    }

}


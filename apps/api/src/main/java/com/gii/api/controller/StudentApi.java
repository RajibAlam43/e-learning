package com.gii.api.controller;

import com.gii.api.model.request.student.CreateCourseReviewRequest;
import com.gii.api.model.request.student.UpdateCourseReviewRequest;
import com.gii.api.model.response.CourseAnnouncementResponse;
import com.gii.api.model.response.CourseReviewResponse;
import com.gii.api.model.response.PageResponse;
import com.gii.api.model.response.student.StudentCertificateSummaryResponse;
import com.gii.api.model.response.student.StudentCollectionDetailsResponse;
import com.gii.api.model.response.student.StudentCollectionSummaryResponse;
import com.gii.api.model.response.student.StudentCompletedLessonsResponse;
import com.gii.api.model.response.student.StudentCourseHomeResponse;
import com.gii.api.model.response.student.StudentCourseSummaryResponse;
import com.gii.api.model.response.student.StudentDashboardResponse;
import com.gii.api.model.response.student.StudentLearningStreakResponse;
import com.gii.api.model.response.student.StudentLiveClassSummaryResponse;
import com.gii.api.model.response.student.StudentOrderSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(
    name = "Student Dashboard",
    description = "Student dashboard, courses, orders, certificates, and live classes")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/student")
@PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
public interface StudentApi {

  @GetMapping("/dashboard")
  @Operation(
      summary = "Get student dashboard",
      description =
          "Get dashboard overview with enrolled courses, progress, certificates,"
              + " and upcoming live classes.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Dashboard retrieved",
            content = @Content(schema = @Schema(implementation = StudentDashboardResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  ResponseEntity<StudentDashboardResponse> getDashboard(Authentication authentication);

  @GetMapping("/progress/completed-lessons")
  @Operation(
      summary = "Get total completed lessons",
      description =
          "Get the student's lifetime completed lesson count across all courses."
              + " Quizzes and live classes are excluded.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Completed lesson total retrieved",
            content =
                @Content(schema = @Schema(implementation = StudentCompletedLessonsResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  ResponseEntity<StudentCompletedLessonsResponse> getCompletedLessons(
      Authentication authentication);

  @GetMapping("/progress/streak")
  @Operation(
      summary = "Get learning streak",
      description =
          "Get the persisted current and maximum daily learning streak. Activity is recorded when"
              + " a lesson is first completed, a quiz attempt is submitted, or a live-class join"
              + " URL is issued.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Learning streak retrieved",
            content =
                @Content(schema = @Schema(implementation = StudentLearningStreakResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  ResponseEntity<StudentLearningStreakResponse> getLearningStreak(Authentication authentication);

  @GetMapping("/announcements")
  @Operation(
      summary = "List course announcements",
      description =
          "List newest announcements from the student's active, unexpired course enrollments.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Announcements retrieved",
            content = @Content(schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  ResponseEntity<PageResponse<CourseAnnouncementResponse>> getCourseAnnouncements(
      @PageableDefault(size = 20) Pageable pageable, Authentication authentication);

  @GetMapping("/courses")
  @Operation(
      summary = "List my courses",
      description = "Get all enrolled/purchased courses with progress and status.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Courses retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  ResponseEntity<List<StudentCourseSummaryResponse>> getMyCourses(Authentication authentication);

  @GetMapping("/collections")
  @Operation(
      summary = "List my collections",
      description = "Get all enrolled collections with overall progress and status.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Collections retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  ResponseEntity<List<StudentCollectionSummaryResponse>> getMyCollections(
      Authentication authentication);

  @GetMapping("/collections/{collectionId}")
  @Operation(
      summary = "Get collection details",
      description =
          "Get enrolled collection details with overall progress and per-course progress.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Collection details retrieved",
            content =
                @Content(
                    schema = @Schema(implementation = StudentCollectionDetailsResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Collection not found or not enrolled")
      })
  ResponseEntity<StudentCollectionDetailsResponse> getMyCollectionDetails(
      @PathVariable UUID collectionId, Authentication authentication);

  @GetMapping("/courses/{courseId}")
  @Operation(
      summary = "Get course details",
      description = "Get detailed course view with sections, lessons, and student's progress.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Course details retrieved",
            content = @Content(schema = @Schema(implementation = StudentCourseHomeResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Course not found or not enrolled")
      })
  ResponseEntity<StudentCourseHomeResponse> getMyCourseDetails(
      @PathVariable UUID courseId, Authentication authentication);

  @PostMapping("/courses/{courseId}/reviews")
  @Operation(summary = "Submit course review")
  ResponseEntity<CourseReviewResponse> createCourseReview(
      @PathVariable UUID courseId,
      @Valid @RequestBody CreateCourseReviewRequest request,
      Authentication authentication);

  @PatchMapping("/courses/{courseId}/reviews")
  @Operation(summary = "Update my course review")
  ResponseEntity<CourseReviewResponse> updateCourseReview(
      @PathVariable UUID courseId,
      @Valid @RequestBody UpdateCourseReviewRequest request,
      Authentication authentication);

  @DeleteMapping("/courses/{courseId}/reviews")
  @Operation(summary = "Delete my course review")
  ResponseEntity<Void> deleteCourseReview(
      @PathVariable UUID courseId, Authentication authentication);

  @GetMapping("/orders")
  @Operation(summary = "List my orders", description = "Get purchase history and order status.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Orders retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  ResponseEntity<List<StudentOrderSummaryResponse>> getMyOrders(Authentication authentication);

  @GetMapping("/certificates")
  @Operation(summary = "List my certificates", description = "Get all earned certificates.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Certificates retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  ResponseEntity<List<StudentCertificateSummaryResponse>> getMyCertificates(
      Authentication authentication);

  @GetMapping("/live-classes")
  @Operation(
      summary = "Get upcoming live classes",
      description = "List upcoming live classes across all enrolled courses.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Live classes retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  ResponseEntity<List<StudentLiveClassSummaryResponse>> getUpcomingLiveClasses(
      Authentication authentication);

  @GetMapping("/courses/{courseId}/live-classes")
  @Operation(
      summary = "Get course live classes",
      description = "List live classes for a specific enrolled course.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Live classes retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Course not found")
      })
  ResponseEntity<List<StudentLiveClassSummaryResponse>> getCourseLiveClasses(
      @PathVariable UUID courseId, Authentication authentication);
}

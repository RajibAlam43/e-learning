package com.gii.api.controller;

import com.gii.api.model.response.student.StudentCertificateSummaryResponse;
import com.gii.api.model.response.student.StudentCourseHomeResponse;
import com.gii.api.model.response.student.StudentCourseSummaryResponse;
import com.gii.api.model.response.student.StudentDashboardResponse;
import com.gii.api.model.response.student.StudentLiveClassJoinResponse;
import com.gii.api.model.response.student.StudentLiveClassSummaryResponse;
import com.gii.api.model.response.student.StudentOrderSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

  @GetMapping("/courses/{courseId}")
  @Operation(
      summary = "Get course details",
      description = "Get detailed course view with sections, lessons, and student's progress.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Course details retrieved",
            content =
                @Content(schema = @Schema(implementation = StudentCourseHomeResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Course not found or not enrolled")
      })
  ResponseEntity<StudentCourseHomeResponse> getMyCourseDetails(
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

  @PostMapping("/live-classes/{liveClassId}/join")
  @Operation(
      summary = "Join live class",
      description = "Check enrollment and get join URL/credentials for a live class.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Join information retrieved",
            content =
                @Content(schema = @Schema(implementation = StudentLiveClassJoinResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Not enrolled or class not yet started"),
        @ApiResponse(responseCode = "404", description = "Live class not found")
      })
  ResponseEntity<StudentLiveClassJoinResponse> joinLiveClass(
      @PathVariable UUID liveClassId, Authentication authentication);
}

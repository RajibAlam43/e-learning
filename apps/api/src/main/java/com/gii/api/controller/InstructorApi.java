package com.gii.api.controller;

import com.gii.api.model.request.instructor.CreateLiveClassRequest;
import com.gii.api.model.request.instructor.UpdateLiveClassRequest;
import com.gii.api.model.response.instructor.InstructorDashboardResponse;
import com.gii.api.model.response.instructor.InstructorLiveClassResponse;
import com.gii.api.model.response.instructor.InstructorLiveClassStartResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(
    name = "Instructor",
    description = "Instructor dashboard, course management, and live class scheduling")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/instructor")
public interface InstructorApi {

  @GetMapping("/dashboard")
  @Operation(
      summary = "Get instructor dashboard",
      description =
          "Get instructor overview with assigned courses, student counts,"
              + " and upcoming live classes.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Dashboard retrieved",
            content =
                @Content(schema = @Schema(implementation = InstructorDashboardResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Not an instructor")
      })
  ResponseEntity<InstructorDashboardResponse> getDashboard(Authentication authentication);

  @PostMapping("/courses/{courseId}/live-classes")
  @Operation(
      summary = "Create live class",
      description = "Schedule a new live class for an instructor-assigned course.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Live class created",
            content =
                @Content(schema = @Schema(implementation = InstructorLiveClassResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input - invalid schedule or lesson"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Not assigned to this course"),
        @ApiResponse(responseCode = "404", description = "Course not found")
      })
  ResponseEntity<InstructorLiveClassResponse> createLiveClass(
      @PathVariable UUID courseId,
      @RequestBody CreateLiveClassRequest request,
      Authentication authentication);

  @PostMapping("/live-classes/{liveClassId}/start")
  @Operation(
      summary = "Start live class",
      description = "Start a live class and get host URLs for Zoom.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Live class started",
            content =
                @Content(
                    schema = @Schema(implementation = InstructorLiveClassStartResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Cannot start - class not scheduled or already completed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Not the instructor of this class"),
        @ApiResponse(responseCode = "404", description = "Live class not found")
      })
  ResponseEntity<InstructorLiveClassStartResponse> startLiveClass(
      @PathVariable UUID liveClassId, Authentication authentication);

  @PatchMapping("/live-classes/{liveClassId}")
  @Operation(
      summary = "Update live class",
      description = "Update live class metadata: title, schedule, or status.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Live class updated",
            content =
                @Content(schema = @Schema(implementation = InstructorLiveClassResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid update data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Not authorized to update this class"),
        @ApiResponse(responseCode = "404", description = "Live class not found")
      })
  ResponseEntity<InstructorLiveClassResponse> updateLiveClass(
      @PathVariable UUID liveClassId,
      @RequestBody UpdateLiveClassRequest request,
      Authentication authentication);

  @DeleteMapping("/live-classes/{liveClassId}")
  @Operation(summary = "Delete live class", description = "Cancel/delete a scheduled live class.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Live class deleted"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Cannot delete - class already started"),
        @ApiResponse(responseCode = "404", description = "Live class not found")
      })
  ResponseEntity<Void> deleteLiveClass(
      @PathVariable UUID liveClassId, Authentication authentication);
}

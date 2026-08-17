package com.gii.api.controller;

import com.gii.api.model.request.instructor.CreateLiveClassRequest;
import com.gii.api.model.request.instructor.UpdateLiveClassRequest;
import com.gii.api.model.response.live.LiveClassStartResponse;
import com.gii.api.model.response.live.LiveClassUpsertResponse;
import com.gii.api.model.response.student.StudentLiveClassJoinResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(
    name = "Live Classes",
    description = "Shared live class operations for admin, instructor, and student")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/live-classes")
public interface LiveClassApi {

  @PostMapping("/courses/{courseId}")
  @PreAuthorize("hasRole('ADMIN') or hasRole('INSTRUCTOR')")
  @Operation(summary = "Create live class", description = "Create a section-level live class.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Live class created",
        content = @Content(schema = @Schema(implementation = LiveClassUpsertResponse.class))),
    @ApiResponse(responseCode = "400", description = "Invalid request"),
    @ApiResponse(responseCode = "403", description = "Forbidden")
  })
  ResponseEntity<LiveClassUpsertResponse> create(
      @PathVariable UUID courseId,
      @RequestBody @Valid CreateLiveClassRequest request,
      Authentication authentication);

  @PatchMapping("/{liveClassId}")
  @PreAuthorize("hasRole('ADMIN') or hasRole('INSTRUCTOR')")
  @Operation(
      summary = "Update live class",
      description = "Update metadata or status for a live class.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Live class updated",
        content = @Content(schema = @Schema(implementation = LiveClassUpsertResponse.class))),
    @ApiResponse(responseCode = "400", description = "Invalid update"),
    @ApiResponse(responseCode = "403", description = "Forbidden")
  })
  ResponseEntity<LiveClassUpsertResponse> update(
      @PathVariable UUID liveClassId,
      @RequestBody @Valid UpdateLiveClassRequest request,
      Authentication authentication);

  @PostMapping("/{liveClassId}/start")
  @PreAuthorize("hasRole('ADMIN') or hasRole('INSTRUCTOR')")
  @Operation(summary = "Start live class", description = "Start a scheduled live class.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Live class started",
        content = @Content(schema = @Schema(implementation = LiveClassStartResponse.class))),
    @ApiResponse(responseCode = "400", description = "Cannot start"),
    @ApiResponse(responseCode = "403", description = "Forbidden")
  })
  ResponseEntity<LiveClassStartResponse> start(
      @PathVariable UUID liveClassId, Authentication authentication);

  @DeleteMapping("/{liveClassId}")
  @PreAuthorize("hasRole('ADMIN') or hasRole('INSTRUCTOR')")
  @Operation(summary = "Cancel live class", description = "Cancel a scheduled live class.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Live class cancelled",
        content = @Content(schema = @Schema(implementation = LiveClassUpsertResponse.class))),
    @ApiResponse(responseCode = "400", description = "Cannot cancel"),
    @ApiResponse(responseCode = "403", description = "Forbidden")
  })
  ResponseEntity<LiveClassUpsertResponse> cancel(
      @PathVariable UUID liveClassId, Authentication authentication);

  @PostMapping("/{liveClassId}/join")
  @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
  @Operation(summary = "Join live class", description = "Join a live class as student/admin.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Join payload returned",
        content = @Content(schema = @Schema(implementation = StudentLiveClassJoinResponse.class))),
    @ApiResponse(responseCode = "403", description = "Forbidden")
  })
  ResponseEntity<StudentLiveClassJoinResponse> join(
      @PathVariable UUID liveClassId, Authentication authentication);
}

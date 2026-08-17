package com.gii.api.controller;

import com.gii.api.model.response.instructor.InstructorDashboardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(
    name = "Instructor",
    description = "Instructor dashboard, course management, and live class scheduling")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/instructor")
@PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
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
}

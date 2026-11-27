package com.gii.api.controller;

import com.gii.api.model.response.academic.AcademicProgressResponse;
import com.gii.api.model.response.academic.ProgramGraduationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Graduation administration")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/admin/students/{userId}")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public interface GraduationAdminApi {
  @GetMapping("/academic-progress")
  @Operation(summary = "Read a student's transcript and program graduation progress")
  ResponseEntity<AcademicProgressResponse> progress(@PathVariable UUID userId);

  @PostMapping("/programs/{programId}/graduation/check")
  @Operation(summary = "Recheck graduation using original requirements; no academic override")
  ResponseEntity<ProgramGraduationResponse> check(
      @PathVariable UUID userId, @PathVariable UUID programId);
}

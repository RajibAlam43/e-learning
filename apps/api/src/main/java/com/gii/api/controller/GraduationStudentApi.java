package com.gii.api.controller;

import com.gii.api.model.response.academic.AcademicProgressResponse;
import com.gii.api.model.response.academic.ProgramGraduationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Graduation and academic progress")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/student")
@PreAuthorize("hasRole('STUDENT')")
public interface GraduationStudentApi {
  @GetMapping("/academic-progress")
  @Operation(summary = "Read your transcript and program graduation progress")
  ResponseEntity<AcademicProgressResponse> progress(Authentication authentication);

  @GetMapping("/collections/{programId}/graduation")
  @Operation(summary = "Read your original graduation requirements and counted credit breakdown")
  ResponseEntity<ProgramGraduationResponse> program(
      @PathVariable UUID programId, Authentication authentication);

  @PostMapping("/collections/{programId}/graduation/check")
  @Operation(summary = "Check requirements and record program completion when eligible")
  ResponseEntity<ProgramGraduationResponse> check(
      @PathVariable UUID programId, Authentication authentication);
}

package com.gii.api.controller;

import com.gii.api.model.response.academic.StudentProgramCurriculumResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Student program curriculum")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/student/collections")
@PreAuthorize("hasRole('STUDENT')")
public interface ProgramCurriculumStudentApi {
  @org.springframework.web.bind.annotation.PostMapping("/{id}/registrations")
  @Operation(summary = "Register for an included course offering using your active program")
  ResponseEntity<com.gii.api.model.response.academic.ProgramRegistrationResponse> register(
      @PathVariable UUID id,
      @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
          com.gii.api.model.request.admin.RegisterProgramCourseRequest request,
      Authentication authentication);

  @GetMapping("/{id}/curriculum")
  @Operation(summary = "Read your original graduation requirements and available program courses")
  ResponseEntity<StudentProgramCurriculumResponse> curriculum(
      @PathVariable UUID id, Authentication authentication);
}

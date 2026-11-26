package com.gii.api.controller;

import com.gii.api.model.response.academic.AcademicRecordResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Academic records")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/student")
@PreAuthorize("hasRole('STUDENT')")
public interface AcademicStudentApi {
  @GetMapping("/academic-record")
  @Operation(summary = "Read your own awarded academic credits")
  ResponseEntity<AcademicRecordResponse> academicRecord(Authentication authentication);
}

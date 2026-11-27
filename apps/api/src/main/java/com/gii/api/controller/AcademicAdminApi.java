package com.gii.api.controller;

import com.gii.api.model.request.admin.RepeatCourseRequest;
import com.gii.api.model.request.admin.SaveSemesterRequest;
import com.gii.api.model.response.academic.AcademicRecordResponse;
import com.gii.api.model.response.academic.SemesterResponse;
import com.gii.api.model.response.admin.AdminCourseDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Academic administration", description = "Institution semesters and academic records")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public interface AcademicAdminApi {
  @GetMapping("/semesters")
  @Operation(summary = "List institution-wide semesters")
  ResponseEntity<List<SemesterResponse>> listSemesters();

  @PostMapping("/semesters")
  @Operation(summary = "Create an institution-wide semester")
  ResponseEntity<SemesterResponse> createSemester(@Valid @RequestBody SaveSemesterRequest request);

  @PutMapping("/semesters/{semesterId}")
  @Operation(summary = "Update a semester without invalidating assigned offerings")
  ResponseEntity<SemesterResponse> updateSemester(
      @PathVariable UUID semesterId, @Valid @RequestBody SaveSemesterRequest request);

  @GetMapping("/students/{userId}/academic-record")
  @Operation(summary = "Read a student's academic credit achievements")
  ResponseEntity<AcademicRecordResponse> academicRecord(@PathVariable UUID userId);

  @GetMapping("/courses/{courseId}/offerings")
  @Operation(summary = "List offerings sharing the same academic course curriculum")
  ResponseEntity<List<AdminCourseDetailResponse>> listOfferings(@PathVariable UUID courseId);

  @PostMapping("/courses/{courseId}/offerings")
  @Operation(
      summary = "Create another cohort using the same curriculum and academic credit identity")
  ResponseEntity<AdminCourseDetailResponse> createOffering(
      @PathVariable UUID courseId,
      @Valid @RequestBody RepeatCourseRequest request,
      Authentication authentication);
}

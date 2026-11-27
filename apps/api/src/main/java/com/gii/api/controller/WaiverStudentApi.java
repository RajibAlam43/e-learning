package com.gii.api.controller;

import com.gii.api.model.request.admin.CreateWaiverRequest;
import com.gii.api.model.request.admin.WaiverEvidenceUploadRequest;
import com.gii.api.model.response.academic.PrerequisiteResponse.CourseOption;
import com.gii.api.model.response.academic.WaiverResponse;
import com.gii.api.service.academic.WaiverEvidenceService;
import com.gii.api.service.storage.R2PresignedUrlService;
import com.gii.common.service.payment.CoursePrerequisiteEligibilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Student prerequisites and waivers")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/student")
@PreAuthorize("hasRole('STUDENT')")
@Validated
public interface WaiverStudentApi {
  @GetMapping("/waivers/courses")
  @Operation(summary = "courses")
  ResponseEntity<List<CourseOption>> courses();

  @GetMapping("/waivers")
  @Operation(summary = "waivers")
  ResponseEntity<List<WaiverResponse>> waivers(
      Authentication authentication, @RequestParam(defaultValue = "0") @Min(0) int page);

  @PostMapping("/waivers")
  @Operation(summary = "request")
  ResponseEntity<WaiverResponse> request(
      Authentication authentication, @Valid @RequestBody CreateWaiverRequest request);

  @PostMapping("/waivers/evidence-upload")
  @Operation(summary = "upload")
  ResponseEntity<WaiverEvidenceService.Upload> upload(
      Authentication authentication, @Valid @RequestBody WaiverEvidenceUploadRequest request);

  @GetMapping("/waivers/{id}/evidence")
  @Operation(summary = "evidence")
  ResponseEntity<R2PresignedUrlService.PresignedDownload> evidence(
      @PathVariable UUID id, Authentication authentication);

  @GetMapping("/courses/{courseId}/eligibility")
  @Operation(summary = "eligibility")
  ResponseEntity<List<CoursePrerequisiteEligibilityService.Requirement>> eligibility(
      @PathVariable UUID courseId, Authentication authentication);
}

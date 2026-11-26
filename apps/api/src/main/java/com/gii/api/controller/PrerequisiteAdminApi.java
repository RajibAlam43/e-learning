package com.gii.api.controller;

import com.gii.api.model.request.admin.ReviewWaiverRequest;
import com.gii.api.model.request.admin.SavePrerequisitesRequest;
import com.gii.api.model.response.academic.PrerequisiteResponse;
import com.gii.api.model.response.academic.WaiverResponse;
import com.gii.api.service.storage.R2PresignedUrlService;
import com.gii.common.enums.WaiverStatus;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Admin prerequisites and waivers")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/admin")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Validated
public interface PrerequisiteAdminApi {
  @GetMapping("/courses/{courseId}/prerequisites")
  @Operation(summary = "prerequisites")
  ResponseEntity<PrerequisiteResponse> prerequisites(@PathVariable UUID courseId);

  @PutMapping("/courses/{courseId}/prerequisites")
  @Operation(summary = "savePrerequisites")
  ResponseEntity<PrerequisiteResponse> savePrerequisites(
      @PathVariable UUID courseId, @Valid @RequestBody SavePrerequisitesRequest request);

  @GetMapping("/waivers")
  @Operation(summary = "waivers")
  ResponseEntity<List<WaiverResponse>> waivers(
      @RequestParam(defaultValue = "PENDING") WaiverStatus status,
      @RequestParam(defaultValue = "0") @Min(0) int page);

  @PostMapping("/waivers/{id}/review")
  @Operation(summary = "review")
  ResponseEntity<WaiverResponse> review(
      @PathVariable UUID id,
      @Valid @RequestBody ReviewWaiverRequest request,
      Authentication authentication);

  @GetMapping("/waivers/{id}/evidence")
  @Operation(summary = "evidence")
  ResponseEntity<R2PresignedUrlService.PresignedDownload> evidence(
      @PathVariable UUID id, Authentication authentication);
}

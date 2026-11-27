package com.gii.api.controller;

import com.gii.api.model.request.admin.ManualEnrollmentRequest;
import com.gii.api.model.response.academic.ManualEnrollmentOverviewResponse;
import com.gii.api.model.response.academic.ManualEnrollmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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

@Tag(name = "Manual enrollment administration")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/admin/students/{userId}/manual-enrollments")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Validated
public interface ManualEnrollmentAdminApi {
  @GetMapping
  @Operation(
      summary = "Read student identity, program entitlements and paged manual enrollment audit")
  ResponseEntity<ManualEnrollmentOverviewResponse> overview(
      @PathVariable UUID userId, @RequestParam(defaultValue = "0") @Min(0) int page);

  @PostMapping
  @Operation(
      summary = "Manually enroll with an audit reason; prerequisites require approved waivers",
      description =
          "Select a course or program, or both with programFunded. "
              + "requestId makes retries idempotent. Existing valid access is returned unchanged. "
              + "Program-funded registrations retain curriculum and elective allowance checks.")
  ResponseEntity<ManualEnrollmentResponse> enroll(
      @PathVariable UUID userId,
      @Valid @RequestBody ManualEnrollmentRequest request,
      Authentication authentication);
}

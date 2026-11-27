package com.gii.api.controller;

import com.gii.api.model.request.admin.ManualEnrollmentRequest;
import com.gii.api.model.response.academic.ManualEnrollmentOverviewResponse;
import com.gii.api.model.response.academic.ManualEnrollmentResponse;
import com.gii.api.service.academic.ManualEnrollmentService;
import com.gii.api.service.enrollment.CurrentUserService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ManualEnrollmentAdminApiController implements ManualEnrollmentAdminApi {
  private final ManualEnrollmentService enrollment;
  private final CurrentUserService users;

  @Override
  public ResponseEntity<ManualEnrollmentOverviewResponse> overview(UUID userId, int page) {
    return ResponseEntity.ok(enrollment.overview(userId, page));
  }

  @Override
  public ResponseEntity<ManualEnrollmentResponse> enroll(
      UUID userId, ManualEnrollmentRequest request, Authentication authentication) {
    return ResponseEntity.ok(
        enrollment.enroll(userId, users.getCurrentUserId(authentication), request));
  }
}

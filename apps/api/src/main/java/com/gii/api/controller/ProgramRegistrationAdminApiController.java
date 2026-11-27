package com.gii.api.controller;

import com.gii.api.model.request.admin.WithdrawProgramRegistrationRequest;
import com.gii.api.model.response.academic.ProgramRegistrationResponse;
import com.gii.api.service.academic.ProgramCourseRegistrationService;
import com.gii.api.service.enrollment.CurrentUserService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProgramRegistrationAdminApiController implements ProgramRegistrationAdminApi {
  private final ProgramCourseRegistrationService registrations;
  private final CurrentUserService users;

  @Override
  public ResponseEntity<List<ProgramRegistrationResponse>> history(UUID userId, int page) {
    return ResponseEntity.ok(registrations.history(userId, page));
  }

  @Override
  public ResponseEntity<ProgramRegistrationResponse> withdraw(
      UUID userId,
      UUID id,
      WithdrawProgramRegistrationRequest request,
      Authentication authentication) {
    return ResponseEntity.ok(
        registrations.withdraw(
            userId, id, users.getCurrentUserId(authentication), request.reason()));
  }
}

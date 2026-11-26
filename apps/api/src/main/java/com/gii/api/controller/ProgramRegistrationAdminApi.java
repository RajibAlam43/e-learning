package com.gii.api.controller;

import com.gii.api.model.request.admin.WithdrawProgramRegistrationRequest;
import com.gii.api.model.response.academic.ProgramRegistrationResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Program registration administration")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/admin/students/{userId}/program-registrations")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@org.springframework.validation.annotation.Validated
public interface ProgramRegistrationAdminApi {
  @GetMapping
  @Operation(summary = "Read a student's latest program registration history")
  ResponseEntity<List<ProgramRegistrationResponse>> history(
      @PathVariable UUID userId,
      @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0")
          @jakarta.validation.constraints.Min(0)
          int page);

  @PostMapping("/{id}/withdraw")
  @Operation(summary = "Withdraw an unfinished course choice with an audit reason")
  ResponseEntity<ProgramRegistrationResponse> withdraw(
      @PathVariable UUID userId,
      @PathVariable UUID id,
      @Valid @RequestBody WithdrawProgramRegistrationRequest request,
      Authentication authentication);
}

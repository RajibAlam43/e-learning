package com.gii.api.controller;

import com.gii.api.model.response.live.LiveClassCoHostResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Live class co-hosts")
public interface LiveClassCoHostApi {
  @PostMapping("/admin/live-classes/{liveClassId}/co-hosts")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  @Operation(
      summary = "Add a Google Meet co-host; omit email to add the current admin",
      description =
          "Preserves course teachers and existing manual co-hosts."
              + " Requires organizer Meet API authorization.")
  ResponseEntity<LiveClassCoHostResponse> add(
      @PathVariable UUID liveClassId,
      @RequestBody(required = false) @Valid AddCoHostRequest request,
      Authentication authentication);

  record AddCoHostRequest(@Email @Size(max = 254) String email) {}
}

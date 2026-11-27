package com.gii.api.model.response.academic;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ManualEnrollmentOverviewResponse(
    UUID userId,
    String fullName,
    String email,
    String phone,
    List<ProgramAccess> programs,
    List<ManualEnrollmentResponse> history) {
  public record ProgramAccess(UUID programId, String title, boolean active, Instant expiresAt) {}
}

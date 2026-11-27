package com.gii.api.model.response.academic;

import com.gii.common.dto.academic.ProgramSnapshot;
import com.gii.common.enums.CollectionType;
import com.gii.common.enums.EnrollmentStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProgramGraduationResponse(
    UUID programId,
    String title,
    CollectionType programType,
    EnrollmentStatus enrollmentStatus,
    boolean accessActive,
    Instant expiresAt,
    Instant completedAt,
    Status status,
    boolean requirementsMet,
    boolean canCheckCompletion,
    boolean canRequestCertificate,
    ProgramSnapshot requirements,
    ProgramCreditProgressResponse creditProgress,
    List<ProgramCreditCourseResponse> courses,
    long creditsRemaining,
    UUID certificateId,
    String certificateCode,
    boolean certificateRevoked) {
  public enum Status {
    COMPLETED,
    ELIGIBLE,
    IN_PROGRESS,
    ACCESS_INACTIVE
  }
}

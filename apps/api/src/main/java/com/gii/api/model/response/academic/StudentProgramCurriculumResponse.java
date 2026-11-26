package com.gii.api.model.response.academic;

import com.gii.common.dto.academic.ProgramSnapshot;
import com.gii.common.enums.PublishStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudentProgramCurriculumResponse(
    ProgramSnapshot graduationRequirements,
    ProgramCreditProgressResponse creditProgress,
    Instant expiresAt,
    boolean accessActive,
    List<AvailableProgramCourse> availableCourses) {
  public record AvailableProgramCourse(
      UUID courseId,
      UUID courseTemplateId,
      String title,
      String slug,
      int credits,
      PublishStatus status,
      boolean mandatoryForGraduation,
      boolean enrolled) {}
}

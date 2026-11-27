package com.gii.api.model.response.academic;

import java.util.List;
import java.util.UUID;

public record AcademicProgressResponse(
    UUID userId,
    String fullName,
    String email,
    String phone,
    AcademicRecordResponse academicRecord,
    List<SemesterLabel> semesters,
    List<ProgramGraduationResponse> programs) {
  public record SemesterLabel(UUID id, String code, String name) {}
}

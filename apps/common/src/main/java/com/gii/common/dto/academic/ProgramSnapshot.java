package com.gii.common.dto.academic;

import com.gii.common.enums.CollectionType;
import java.util.List;
import java.util.UUID;

/** Immutable purchase contract, separate from the program's evolving course catalogue. */
public record ProgramSnapshot(
    CollectionType collectionType,
    Integer requiredCredits,
    int electiveCreditLimit,
    Integer expectedDurationMonths,
    Integer accessDurationMonths,
    List<CourseRequirement> courses) {
  public ProgramSnapshot {
    courses = List.copyOf(courses);
  }

  public boolean creditBased() {
    return requiredCredits != null;
  }

  public record CourseRequirement(
      UUID courseId,
      UUID courseTemplateId,
      String title,
      int credits,
      boolean mandatory,
      int position) {}
}

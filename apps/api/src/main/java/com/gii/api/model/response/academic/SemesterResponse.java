package com.gii.api.model.response.academic;

import com.gii.common.entity.academic.Semester;
import java.time.Instant;
import java.util.UUID;

public record SemesterResponse(
    UUID semesterId,
    String code,
    String name,
    Instant startsAt,
    Instant endsAt,
    Instant registrationStartsAt,
    Instant registrationEndsAt,
    String status) {
  public static SemesterResponse from(Semester semester) {
    Instant now = Instant.now();
    String status =
        !now.isBefore(semester.getEndsAt())
            ? "COMPLETED"
            : !now.isBefore(semester.getStartsAt())
                ? "ACTIVE"
                : !now.isBefore(semester.getRegistrationStartsAt())
                        && now.isBefore(semester.getRegistrationEndsAt())
                    ? "REGISTRATION_OPEN"
                    : "PLANNED";
    return new SemesterResponse(
        semester.getId(),
        semester.getCode(),
        semester.getName(),
        semester.getStartsAt(),
        semester.getEndsAt(),
        semester.getRegistrationStartsAt(),
        semester.getRegistrationEndsAt(),
        status);
  }
}

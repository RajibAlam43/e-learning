package com.gii.api.model.response.academic;

import com.gii.common.enums.StudyMode;
import com.gii.common.service.payment.CoursePrerequisiteEligibilityService.Requirement;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProgramOfferingResponse(
    UUID courseId,
    String name,
    String slug,
    StudyMode studyMode,
    SemesterResponse semester,
    Instant startsAt,
    Instant endsAt,
    String timezone,
    int credits,
    Instant accessExpiresAt,
    Long seatsAvailable,
    boolean canRegister,
    String blockedReason,
    List<Requirement> missingPrerequisites) {}

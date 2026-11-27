package com.gii.api.model.response.academic;

import com.gii.common.enums.AcademicAchievementSource;
import java.time.Instant;
import java.util.UUID;

public record ProgramCreditCourseResponse(
    UUID courseTemplateId,
    String title,
    boolean mandatory,
    int expectedCredits,
    int awardedCredits,
    int countedCredits,
    AcademicAchievementSource source,
    Instant achievedAt) {}

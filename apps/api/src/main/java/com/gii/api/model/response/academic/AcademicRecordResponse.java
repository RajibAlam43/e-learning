package com.gii.api.model.response.academic;

import com.gii.common.enums.AcademicAchievementSource;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AcademicRecordResponse(
    UUID userId, long totalCredits, List<Achievement> achievements) {
  public record Achievement(
      UUID achievementId,
      UUID courseTemplateId,
      UUID courseId,
      UUID semesterId,
      String courseTitle,
      int credits,
      AcademicAchievementSource source,
      Instant achievedAt) {}
}

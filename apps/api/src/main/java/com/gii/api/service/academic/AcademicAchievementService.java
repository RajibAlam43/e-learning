package com.gii.api.service.academic;

import com.gii.api.model.response.academic.AcademicRecordResponse;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.repository.academic.AcademicAchievementRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AcademicAchievementService {
  private final AcademicAchievementRepository achievements;

  @Transactional
  public void awardCompletion(Enrollment enrollment) {
    if (enrollment.getCompletedAt() == null) {
      throw new IllegalArgumentException("Only completed enrollments can receive academic credits");
    }
    achievements.awardCompletion(enrollment.getId());
  }

  @Transactional(readOnly = true)
  public AcademicRecordResponse get(UUID userId) {
    var items =
        achievements.findByUserIdOrderByAchievedAtDesc(userId).stream()
            .map(item ->
                new AcademicRecordResponse.Achievement(
                    item.getId(),
                    item.getCourseTemplateId(),
                    item.getSourceCourseId(),
                    item.getSemesterId(),
                    item.getCourseTitle(),
                    item.getCredits(),
                    item.getSource(),
                    item.getAchievedAt()))
            .toList();
    return new AcademicRecordResponse(
        userId, items.stream().mapToLong(item -> item.credits()).sum(), items);
  }
}

package com.gii.api.service.progress;

import com.gii.api.model.response.academic.ProgramCreditProgressResponse;
import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.repository.academic.AcademicAchievementRepository;
import com.gii.common.repository.collection.CollectionCourseRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgramCreditProgressService {
  private final AcademicAchievementRepository achievements;
  private final CollectionCourseRepository courses;
  private final com.gii.common.repository.academic.ProgramCourseRegistrationRepository
      registrations;

  public ProgramCreditProgressResponse get(CollectionEnrollment enrollment) {
    var snapshot = enrollment.getProgramSnapshot();
    if (snapshot == null || !snapshot.creditBased()) {
      return null;
    }
    var mandatory = new HashSet<UUID>();
    var eligible = new HashSet<UUID>();
    for (var c : snapshot.courses()) {
      eligible.add(c.courseTemplateId());
      if (c.mandatory()) {
        mandatory.add(c.courseTemplateId());
      }
    }
    courses
        .findByCollection_IdOrderByPositionAsc(enrollment.getCollection().getId())
        .forEach(c -> eligible.add(c.getCourse().getTemplate().getId()));
    registrations
        .findByProgramEnrollmentIdAndStatus(
            enrollment.getId(), com.gii.common.enums.ProgramRegistrationStatus.ACTIVE)
        .forEach(r -> eligible.add(r.getCourseTemplateId()));
    var awards = new HashMap<UUID, Integer>();
    achievements
        .findByUserIdOrderByAchievedAtDesc(enrollment.getUser().getId())
        .forEach(a -> awards.put(a.getCourseTemplateId(), a.getCredits()));
    long core = 0;
    long elective = 0;
    int completed = 0;
    for (var id : eligible) {
      int credits = awards.getOrDefault(id, 0);
      if (mandatory.contains(id)) {
        core += credits;
        if (credits > 0) {
          completed++;
        }
      } else {
        elective += credits;
      }
    }
    long counted = Math.min(elective, snapshot.electiveCreditLimit());
    long earned = core + counted;
    return new ProgramCreditProgressResponse(
        snapshot.requiredCredits(),
        earned,
        counted,
        snapshot.electiveCreditLimit(),
        completed,
        mandatory.size(),
        completed == mandatory.size() && earned >= snapshot.requiredCredits());
  }
}

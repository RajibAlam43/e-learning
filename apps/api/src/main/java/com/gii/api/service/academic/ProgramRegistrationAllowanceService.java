package com.gii.api.service.academic;

import com.gii.api.model.response.academic.ProgramAllowanceResponse;
import com.gii.common.entity.academic.ProgramCourseRegistration;
import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.repository.academic.AcademicAchievementRepository;
import com.gii.common.repository.collection.CollectionCourseRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgramRegistrationAllowanceService {
  private final AcademicAchievementRepository achievements;
  private final CollectionCourseRepository courses;

  public ProgramAllowanceResponse get(
      CollectionEnrollment program, List<ProgramCourseRegistration> active) {
    var snapshot = program.getProgramSnapshot();
    if (snapshot == null || !snapshot.creditBased()) {
      return new ProgramAllowanceResponse(null, 0, null);
    }
    var mandatory = new HashSet<UUID>();
    var eligible = new HashSet<UUID>();
    snapshot
        .courses()
        .forEach(c -> {
          eligible.add(c.courseTemplateId());
          if (c.mandatory()) {
            mandatory.add(c.courseTemplateId());
          }
        });
    courses
        .findByCollection_IdOrderByPositionAsc(program.getCollection().getId())
        .forEach(c -> eligible.add(c.getCourse().getTemplate().getId()));
    var used = new HashMap<UUID, Integer>();
    for (var r : active) {
      eligible.add(r.getCourseTemplateId());
      if (!r.isMandatory()) {
        used.put(r.getCourseTemplateId(), r.getCredits());
      }
    }
    // A reused achievement and its selected course occupy one allowance slot, not two.
    for (var a : achievements.findByUserIdOrderByAchievedAtDesc(program.getUser().getId())) {
      if (eligible.contains(a.getCourseTemplateId())
          && !mandatory.contains(a.getCourseTemplateId())) {
        used.putIfAbsent(a.getCourseTemplateId(), a.getCredits());
      }
    }
    long count = used.values().stream().mapToLong(Integer::longValue).sum();
    return new ProgramAllowanceResponse(
        snapshot.electiveCreditLimit(),
        Math.min(count, snapshot.electiveCreditLimit()),
        Math.max(0, snapshot.electiveCreditLimit() - count));
  }
}

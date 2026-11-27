package com.gii.api.service.progress;

import com.gii.api.model.response.academic.ProgramCreditCourseResponse;
import com.gii.api.model.response.academic.ProgramCreditProgressResponse;
import com.gii.common.dto.academic.ProgramSnapshot.CourseRequirement;
import com.gii.common.entity.academic.AcademicAchievement;
import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.enums.ProgramRegistrationStatus;
import com.gii.common.repository.academic.AcademicAchievementRepository;
import com.gii.common.repository.academic.ProgramCourseRegistrationRepository;
import com.gii.common.repository.collection.CollectionCourseRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
  private final ProgramCourseRegistrationRepository registrations;

  public record Breakdown(
      ProgramCreditProgressResponse progress, List<ProgramCreditCourseResponse> courses) {}

  public ProgramCreditProgressResponse get(CollectionEnrollment enrollment) {
    var result = breakdown(enrollment);
    return result == null ? null : result.progress();
  }

  public Breakdown breakdown(CollectionEnrollment enrollment) {
    var snapshot = enrollment.getProgramSnapshot();
    if (snapshot == null || !snapshot.creditBased()) {
      return null;
    }
    var eligible = new LinkedHashMap<UUID, CourseRequirement>();
    snapshot.courses().forEach(c -> eligible.put(c.courseTemplateId(), c));
    courses
        .findByCollection_IdOrderByPositionAsc(enrollment.getCollection().getId())
        .forEach(row -> {
          var c = row.getCourse();
          eligible.putIfAbsent(
              c.getTemplate().getId(),
              new CourseRequirement(
                  c.getId(),
                  c.getTemplate().getId(),
                  c.getTitle(),
                  c.getCredits(),
                  false,
                  row.getPosition()));
        });
    registrations
        .findByProgramEnrollmentIdAndStatus(enrollment.getId(), ProgramRegistrationStatus.ACTIVE)
        .forEach(r ->
            eligible.putIfAbsent(
                r.getCourseTemplateId(),
                new CourseRequirement(
                    r.getCourseId(),
                    r.getCourseTemplateId(),
                    r.getEnrollment().getCourse().getTitle(),
                    r.getCredits(),
                    false,
                    eligible.size() + 1)));
    var awards = new HashMap<UUID, AcademicAchievement>();
    achievements
        .findByUserIdOrderByAchievedAtDesc(enrollment.getUser().getId())
        .forEach(a -> awards.put(a.getCourseTemplateId(), a));
    var countedElectives = new HashMap<UUID, Integer>();
    int remaining = snapshot.electiveCreditLimit();
    // Stable allocation explains exactly which awards occupy a partly used elective cap.
    var electiveAwards =
        awards.values().stream()
            .filter(a ->
                eligible.containsKey(a.getCourseTemplateId())
                    && !eligible.get(a.getCourseTemplateId()).mandatory())
            .sorted(
                Comparator.comparing(AcademicAchievement::getAchievedAt)
                    .thenComparing(AcademicAchievement::getCourseTemplateId))
            .toList();
    for (var award : electiveAwards) {
      int counted = Math.min(remaining, award.getCredits());
      countedElectives.put(award.getCourseTemplateId(), counted);
      remaining -= counted;
    }
    var rows =
        eligible.values().stream()
            .map(c -> {
              var a = awards.get(c.courseTemplateId());
              int credited = a == null ? 0 : a.getCredits();
              return new ProgramCreditCourseResponse(
                  c.courseTemplateId(),
                  c.title(),
                  c.mandatory(),
                  c.credits(),
                  credited,
                  c.mandatory() ? credited : countedElectives.getOrDefault(c.courseTemplateId(), 0),
                  a == null ? null : a.getSource(),
                  a == null ? null : a.getAchievedAt());
            })
            .toList();
    long earned = rows.stream().mapToLong(ProgramCreditCourseResponse::countedCredits).sum();
    long electives =
        rows.stream()
            .filter(c -> !c.mandatory())
            .mapToLong(ProgramCreditCourseResponse::countedCredits)
            .sum();
    int mandatory = (int) rows.stream().filter(ProgramCreditCourseResponse::mandatory).count();
    int completed =
        (int) rows.stream().filter(c -> c.mandatory() && c.awardedCredits() > 0).count();
    return new Breakdown(
        new ProgramCreditProgressResponse(
            snapshot.requiredCredits(),
            earned,
            electives,
            snapshot.electiveCreditLimit(),
            completed,
            mandatory,
            completed == mandatory && earned >= snapshot.requiredCredits()),
        rows);
  }
}

package com.gii.common.service.payment;

import com.gii.common.dto.academic.ProgramSnapshot;
import com.gii.common.entity.collection.Collection;
import com.gii.common.entity.collection.CollectionCourse;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProgramSnapshotService {
  public static java.time.Instant expiresAt(com.gii.common.entity.order.OrderItem item) {
    var snapshot = item.getProgramSnapshot();
    if (snapshot == null || !snapshot.creditBased()) {
      return null;
    }
    var paidAt = item.getOrder().getPaidAt();
    if (paidAt == null) {
      throw new EnrollmentPolicyViolationException("Program payment confirmation time is required");
    }
    return paidAt
        .atZone(java.time.ZoneOffset.UTC)
        .plusMonths(snapshot.accessDurationMonths())
        .toInstant();
  }

  public ProgramSnapshot capture(Collection collection, List<CollectionCourse> courses) {
    return new ProgramSnapshot(
        collection.getType(),
        collection.getRequiredCredits(),
        collection.getElectiveCreditLimit(),
        collection.getExpectedDurationMonths(),
        collection.isCreditBased()
            ? (collection.getAccessDurationMonths() == null
                ? collection.getExpectedDurationMonths() + 24
                : collection.getAccessDurationMonths())
            : null,
        courses.stream()
            .sorted(Comparator.comparing(CollectionCourse::getPosition))
            .map(row -> {
              var c = row.getCourse();
              var t = c.getTemplate();
              return new ProgramSnapshot.CourseRequirement(
                  c.getId(),
                  t.getId(),
                  t.getTitleEn() != null && !t.getTitleEn().isBlank()
                      ? t.getTitleEn()
                      : t.getTitle(),
                  t.getCredits(),
                  !Boolean.FALSE.equals(row.getIsMandatory()),
                  row.getPosition());
            })
            .toList());
  }

  public void validate(ProgramSnapshot snapshot) {
    if (!snapshot.creditBased()) {
      return;
    }
    if (snapshot.courses().isEmpty()) {
      throw new EnrollmentPolicyViolationException("Add at least one course to the program");
    }
    long distinct =
        snapshot.courses().stream()
            .map(ProgramSnapshot.CourseRequirement::courseTemplateId)
            .distinct()
            .count();
    if (distinct != snapshot.courses().size()) {
      throw new EnrollmentPolicyViolationException(
          "Include each course curriculum only once; cohorts share one credit identity");
    }
    long mandatory =
        snapshot.courses().stream()
            .filter(ProgramSnapshot.CourseRequirement::mandatory)
            .mapToLong(ProgramSnapshot.CourseRequirement::credits)
            .sum();
    long electives =
        snapshot.courses().stream()
            .filter(c -> !c.mandatory())
            .mapToLong(ProgramSnapshot.CourseRequirement::credits)
            .sum();
    if (snapshot.requiredCredits() < mandatory) {
      throw new EnrollmentPolicyViolationException(
          "Required credits cannot be less than the mandatory course credits");
    }
    if (snapshot.requiredCredits()
        > mandatory + Math.min(electives, snapshot.electiveCreditLimit())) {
      throw new EnrollmentPolicyViolationException(
          "Required credits cannot be reached within the elective credit allowance");
    }
  }
}

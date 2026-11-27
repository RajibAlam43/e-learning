package com.gii.api.service.progress;

import com.gii.api.service.collection.PurchasedCollectionCoursesService;
import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.service.payment.CoursePrerequisiteEligibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionRequirementsCompletionService {
  private final PurchasedCollectionCoursesService purchased;
  private final CourseCompletionService completion;
  private final CoursePrerequisiteEligibilityService eligibility;
  private final ProgramCreditProgressService programProgress;

  public boolean isComplete(CollectionEnrollment enrollment) {
    if (enrollment.getProgramSnapshot() != null && enrollment.getProgramSnapshot().creditBased()) {
      return programProgress.get(enrollment).complete();
    }
    var required =
        purchased.resolveRequiredItems(enrollment).stream()
            .filter(PurchasedCollectionCoursesService.PurchasedCourse::mandatory)
            .toList();
    if (required.isEmpty()) {
      return false;
    }
    var remaining =
        required.stream()
            .map(item -> item.course())
            .filter(
                c -> !eligibility.hasCredit(enrollment.getUser().getId(), c.getTemplate().getId()))
            .map(c -> c.getId())
            .distinct()
            .toList();
    if (remaining.isEmpty()) {
      return true;
    }
    var progress = completion.getByCourseIds(enrollment.getUser().getId(), remaining);
    return remaining.stream()
        .allMatch(id -> {
          var p = progress.get(id);
          return p != null
              && (p.permanentlyCompleted()
                  || p.totalItems() > 0 && p.completedItems() >= p.totalItems());
        });
  }
}

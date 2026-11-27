package com.gii.api.service.progress;

import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mirrors {@link EnrollmentCompletionService} for collections: once a collection enrollment's
 * completedAt is set, it is never recomputed. It is set the moment every mandatory course in the
 * collection is complete, whether that happens directly (checked here) or indirectly (a course
 * inside the collection just flipped to complete — see {@link #refreshCollectionsForCourse}).
 */
@Service
@RequiredArgsConstructor
public class CollectionEnrollmentCompletionService {

  private final CollectionEnrollmentRepository collectionEnrollmentRepository;
  private final CollectionRequirementsCompletionService requirements;

  @Transactional
  public void refresh(UUID userId, UUID collectionId) {
    CollectionEnrollment enrollment =
        collectionEnrollmentRepository
            .findByUserIdAndCollectionIdForUpdate(userId, collectionId)
            .orElse(null);
    if (enrollment == null
        || !enrollment.isAccessActiveAt(Instant.now())
        || enrollment.getCompletedAt() != null) {
      return;
    }
    if (requirements.isComplete(enrollment)) {
      enrollment.setCompletedAt(Instant.now());
      collectionEnrollmentRepository.save(enrollment);
    }
  }

  /** Called after a single course flips to complete, to re-check every collection it belongs to. */
  @Transactional
  public void refreshCollectionsForCourse(UUID userId, UUID courseId) {
    for (var enrollment :
        collectionEnrollmentRepository.findByUserIdAndStatus(userId, EnrollmentStatus.ACTIVE)) {
      refresh(userId, enrollment.getCollection().getId());
    }
  }
}

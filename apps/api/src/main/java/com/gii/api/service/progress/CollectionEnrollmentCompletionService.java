package com.gii.api.service.progress;

import com.gii.api.service.collection.PurchasedCollectionCoursesService;
import com.gii.api.service.collection.PurchasedCollectionCoursesService.PurchasedCourse;
import com.gii.api.service.progress.CourseCompletionService.CourseCompletion;
import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.repository.collection.CollectionCourseRepository;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
  private final CollectionCourseRepository collectionCourseRepository;
  private final CourseCompletionService courseCompletionService;
  private final PurchasedCollectionCoursesService purchasedCollectionCoursesService;

  @Transactional
  public void refresh(UUID userId, UUID collectionId) {
    CollectionEnrollment enrollment =
        collectionEnrollmentRepository
            .findByUserIdAndCollectionIdForUpdate(userId, collectionId)
            .orElse(null);
    if (enrollment == null
        || enrollment.getStatus() != EnrollmentStatus.ACTIVE
        || enrollment.getCompletedAt() != null) {
      return;
    }
    List<UUID> courseIds =
        purchasedCollectionCoursesService.resolveItems(enrollment).stream()
            .filter(PurchasedCourse::mandatory)
            .map(item -> item.course().getId())
            .distinct()
            .toList();
    if (courseIds.isEmpty()) {
      return;
    }
    Map<UUID, CourseCompletion> completions =
        courseCompletionService.getByCourseIds(userId, courseIds);
    int totalItems = completions.values().stream().mapToInt(CourseCompletion::totalItems).sum();
    int completedItems =
        completions.values().stream().mapToInt(CourseCompletion::completedItems).sum();
    if (totalItems > 0 && completedItems >= totalItems) {
      enrollment.setCompletedAt(Instant.now());
      collectionEnrollmentRepository.save(enrollment);
    }
  }

  /** Called after a single course flips to complete, to re-check every collection it belongs to. */
  @Transactional
  public void refreshCollectionsForCourse(UUID userId, UUID courseId) {
    for (var collectionCourse : collectionCourseRepository.findByCourse_Id(courseId)) {
      UUID collectionId = collectionCourse.getCollection().getId();
      if (collectionEnrollmentRepository.existsByUserIdAndCollectionIdAndStatus(
          userId, collectionId, EnrollmentStatus.ACTIVE)) {
        refresh(userId, collectionId);
      }
    }
  }
}

package com.gii.api.service.student;

import com.gii.api.model.response.student.StudentCollectionSummaryResponse;
import com.gii.api.service.collection.PurchasedCollectionCoursesService;
import com.gii.api.service.collection.PurchasedCollectionCoursesService.PurchasedCourse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.api.service.localization.LocalizedContentService;
import com.gii.api.service.progress.CourseCompletionService;
import com.gii.api.service.progress.CourseCompletionService.CourseCompletion;
import com.gii.api.service.storage.AssetUrlService;
import com.gii.common.entity.collection.Collection;
import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.entity.course.Course;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.collection.CollectionCourseRepository;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentCollectionsService {

  private final CurrentUserService currentUserService;
  private final CollectionEnrollmentRepository collectionEnrollmentRepository;
  private final CollectionCourseRepository collectionCourseRepository;
  private final CourseCompletionService courseCompletionService;
  private final AssetUrlService assetUrlService;
  private final LocalizedContentService localizedContentService;
  private final PurchasedCollectionCoursesService purchasedCollectionCoursesService;

  public List<StudentCollectionSummaryResponse> execute(Authentication authentication) {
    UUID userId = currentUserService.getCurrentUserId(authentication);
    List<CollectionEnrollment> enrollments =
        collectionEnrollmentRepository.findByUserIdAndStatus(userId, EnrollmentStatus.ACTIVE);
    if (enrollments.isEmpty()) {
      return List.of();
    }

    Map<UUID, List<PurchasedCourse>> coursesByCollectionId = new HashMap<>();
    for (CollectionEnrollment enrollment : enrollments) {
      coursesByCollectionId.put(
          enrollment.getCollection().getId(),
          purchasedCollectionCoursesService.resolveItems(enrollment).stream()
              .filter(item -> item.course().getStatus() == PublishStatus.PUBLISHED)
              .toList());
    }

    List<UUID> allCourseIds =
        coursesByCollectionId.values().stream()
            .flatMap(List::stream)
            .map(item -> item.course().getId())
            .distinct()
            .toList();
    Map<UUID, CourseCompletion> completionByCourseId =
        courseCompletionService.getByCourseIds(userId, allCourseIds);

    return enrollments.stream()
        .map(
            enrollment -> {
              Collection collection = enrollment.getCollection();
              List<PurchasedCourse> collectionCourses =
                  coursesByCollectionId.getOrDefault(collection.getId(), List.of());
              int totalLessons = 0;
              int completedLessons = 0;
              int totalItems = 0;
              int completedItems = 0;
              for (PurchasedCourse item : collectionCourses) {
                Course course = item.course();
                UUID courseId = course.getId();
                CourseCompletion courseCompletion = completionByCourseId.get(courseId);
                if (item.mandatory()) {
                  totalLessons += courseCompletion.totalLessons();
                  completedLessons += courseCompletion.completedLessons();
                  totalItems += courseCompletion.totalItems();
                  completedItems += courseCompletion.completedItems();
                }
              }
              double progress =
                  totalItems == 0 ? 0.0 : Math.round(completedItems * 10000.0 / totalItems) / 100.0;
              return StudentCollectionSummaryResponse.builder()
                  .collectionId(collection.getId())
                  .collectionName(
                      localizedContentService.text(collection.getTitle(), collection.getTitleEn()))
                  .collectionSlug(collection.getSlug())
                  .collectionType(collection.getType())
                  .thumbnailUrl(assetUrlService.publicUrl(collection.getThumbnailObjectKey()))
                  .progressPercentage(progress)
                  .completedLessons(completedLessons)
                  .totalLessons(totalLessons)
                  .completedItems(completedItems)
                  .totalItems(totalItems)
                  .courseCount(collectionCourses.size())
                  .enrollmentStatus(enrollment.getStatus())
                  .enrolledAt(enrollment.getEnrolledAt())
                  .completedAt(enrollment.getCompletedAt())
                  .expiresAt(enrollment.getExpiresAt())
                  .build();
            })
        .toList();
  }
}

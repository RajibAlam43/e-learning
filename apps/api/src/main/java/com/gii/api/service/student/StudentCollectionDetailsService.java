package com.gii.api.service.student;

import com.gii.api.model.response.student.StudentCollectionCourseProgressResponse;
import com.gii.api.model.response.student.StudentCollectionDetailsResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.api.service.localization.LocalizedContentService;
import com.gii.api.service.progress.CourseCompletionService;
import com.gii.api.service.progress.CourseCompletionService.CourseCompletion;
import com.gii.api.service.storage.AssetUrlService;
import com.gii.common.entity.collection.Collection;
import com.gii.common.entity.collection.CollectionCourse;
import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.collection.CollectionCourseRepository;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentCollectionDetailsService {

  private final CurrentUserService currentUserService;
  private final CollectionEnrollmentRepository collectionEnrollmentRepository;
  private final CollectionCourseRepository collectionCourseRepository;
  private final CourseCompletionService courseCompletionService;
  private final AssetUrlService assetUrlService;
  private final LocalizedContentService localizedContentService;

  public StudentCollectionDetailsResponse execute(
      UUID collectionId, Authentication authentication) {
    UUID userId = currentUserService.getCurrentUserId(authentication);
    CollectionEnrollment enrollment =
        collectionEnrollmentRepository
            .findByUserIdAndCollectionIdAndStatus(userId, collectionId, EnrollmentStatus.ACTIVE)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Collection not found or not enrolled"));

    Collection collection = enrollment.getCollection();
    List<CollectionCourse> collectionCourses =
        collectionCourseRepository.findByCollection_IdOrderByPositionAscWithCourseStatus(
            collectionId, PublishStatus.PUBLISHED);

    List<UUID> courseIds = collectionCourses.stream().map(cc -> cc.getCourse().getId()).toList();
    Map<UUID, CourseCompletion> completionByCourseId =
        courseCompletionService.getByCourseIds(userId, courseIds);

    int totalLessons = 0;
    int completedLessons = 0;
    List<StudentCollectionCourseProgressResponse> courses =
        collectionCourses.stream()
            .map(
                cc -> {
                  UUID courseId = cc.getCourse().getId();
                  CourseCompletion completion = completionByCourseId.get(courseId);
                  return StudentCollectionCourseProgressResponse.builder()
                      .courseId(courseId)
                      .courseName(
                          localizedContentService.text(
                              cc.getCourse().getTitle(), cc.getCourse().getTitleEn()))
                      .courseSlug(cc.getCourse().getSlug())
                      .courseThumbnailUrl(
                          assetUrlService.publicUrl(cc.getCourse().getThumbnailObjectKey()))
                      .completionPercentage(completion.completionPercentage())
                      .completedLessons(completion.completedLessons())
                      .totalLessons(completion.totalLessons())
                      .completedItems(completion.completedItems())
                      .totalItems(completion.totalItems())
                      .build();
                })
            .toList();

    for (StudentCollectionCourseProgressResponse item : courses) {
      totalLessons += item.totalLessons();
      completedLessons += item.completedLessons();
    }
    int totalItems =
        courses.stream().mapToInt(StudentCollectionCourseProgressResponse::totalItems).sum();
    int completedItems =
        courses.stream().mapToInt(StudentCollectionCourseProgressResponse::completedItems).sum();
    double progress =
        totalItems == 0 ? 0.0 : Math.round(completedItems * 10000.0 / totalItems) / 100.0;

    return StudentCollectionDetailsResponse.builder()
        .collectionId(collection.getId())
        .collectionName(
            localizedContentService.text(collection.getTitle(), collection.getTitleEn()))
        .collectionSlug(collection.getSlug())
        .collectionType(collection.getType())
        .thumbnailUrl(assetUrlService.publicUrl(collection.getThumbnailObjectKey()))
        .shortDescription(
            localizedContentService.text(
                collection.getShortDescription(), collection.getShortDescriptionEn()))
        .description(
            localizedContentService.text(
                collection.getDescription(), collection.getDescriptionEn()))
        .progressPercentage(progress)
        .completedLessons(completedLessons)
        .totalLessons(totalLessons)
        .completedItems(completedItems)
        .totalItems(totalItems)
        .courseCount(courses.size())
        .enrollmentStatus(enrollment.getStatus())
        .enrolledAt(enrollment.getEnrolledAt())
        .completedAt(enrollment.getCompletedAt())
        .expiresAt(enrollment.getExpiresAt())
        .courses(courses)
        .build();
  }
}

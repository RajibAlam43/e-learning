package com.gii.api.service.student;

import com.gii.api.model.response.student.StudentCollectionCourseProgressResponse;
import com.gii.api.model.response.student.StudentCollectionDetailsResponse;
import com.gii.api.service.course.CourseThumbnailUrlService;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.entity.collection.Collection;
import com.gii.common.entity.collection.CollectionCourse;
import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.collection.CollectionCourseRepository;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.enrollment.LessonProgressRepository;
import java.util.HashMap;
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
  private final LessonRepository lessonRepository;
  private final LessonProgressRepository lessonProgressRepository;
  private final CourseThumbnailUrlService courseThumbnailUrlService;

  public StudentCollectionDetailsResponse execute(UUID collectionId, Authentication authentication) {
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
    Map<UUID, Integer> totalLessonsByCourseId = lessonCountByCourseId(courseIds);
    Map<UUID, Integer> completedLessonsByCourseId = completedLessonCountByCourseId(userId, courseIds);

    int totalLessons = 0;
    int completedLessons = 0;
    List<StudentCollectionCourseProgressResponse> courses =
        collectionCourses.stream()
            .map(
                cc -> {
                  UUID courseId = cc.getCourse().getId();
                  int total = totalLessonsByCourseId.getOrDefault(courseId, 0);
                  int completed = completedLessonsByCourseId.getOrDefault(courseId, 0);
                  double progress = total == 0 ? 0.0 : (completed * 100.0) / total;
                  return StudentCollectionCourseProgressResponse.builder()
                      .courseId(courseId)
                      .courseName(cc.getCourse().getTitle())
                      .courseSlug(cc.getCourse().getSlug())
                      .courseThumbnailUrl(courseThumbnailUrlService.buildCourseThumbnailUrl(cc.getCourse()))
                      .completionPercentage(round2(progress))
                      .completedLessons(completed)
                      .totalLessons(total)
                      .build();
                })
            .toList();

    for (StudentCollectionCourseProgressResponse item : courses) {
      totalLessons += item.totalLessons();
      completedLessons += item.completedLessons();
    }
    double progress = totalLessons == 0 ? 0.0 : (completedLessons * 100.0) / totalLessons;

    return StudentCollectionDetailsResponse.builder()
        .collectionId(collection.getId())
        .collectionName(collection.getTitle())
        .collectionSlug(collection.getSlug())
        .collectionType(collection.getType())
        .thumbnailObjectKey(collection.getThumbnailObjectKey())
        .shortDescription(collection.getShortDescription())
        .description(collection.getDescription())
        .progressPercentage(round2(progress))
        .completedLessons(completedLessons)
        .totalLessons(totalLessons)
        .courseCount(courses.size())
        .enrollmentStatus(enrollment.getStatus())
        .enrolledAt(enrollment.getEnrolledAt())
        .completedAt(enrollment.getCompletedAt())
        .expiresAt(enrollment.getExpiresAt())
        .courses(courses)
        .build();
  }

  private Map<UUID, Integer> lessonCountByCourseId(List<UUID> courseIds) {
    if (courseIds.isEmpty()) {
      return Map.of();
    }
    Map<UUID, Integer> counts = new HashMap<>();
    for (Object[] row : lessonRepository.countByCourseIdsAndStatus(courseIds, PublishStatus.PUBLISHED)) {
      counts.put((UUID) row[0], ((Long) row[1]).intValue());
    }
    return counts;
  }

  private Map<UUID, Integer> completedLessonCountByCourseId(UUID userId, List<UUID> courseIds) {
    if (courseIds.isEmpty()) {
      return Map.of();
    }
    Map<UUID, Integer> counts = new HashMap<>();
    for (Object[] row : lessonProgressRepository.countCompletedByUserIdAndCourseIds(userId, courseIds)) {
      counts.put((UUID) row[0], ((Long) row[1]).intValue());
    }
    return counts;
  }

  private double round2(double value) {
    return Math.round(value * 100.0) / 100.0;
  }
}

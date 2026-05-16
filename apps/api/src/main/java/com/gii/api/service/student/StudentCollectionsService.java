package com.gii.api.service.student;

import com.gii.api.model.response.student.StudentCollectionSummaryResponse;
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
  private final LessonRepository lessonRepository;
  private final LessonProgressRepository lessonProgressRepository;

  public List<StudentCollectionSummaryResponse> execute(Authentication authentication) {
    UUID userId = currentUserService.getCurrentUserId(authentication);
    List<CollectionEnrollment> enrollments =
        collectionEnrollmentRepository.findByUserIdAndStatus(userId, EnrollmentStatus.ACTIVE);
    if (enrollments.isEmpty()) {
      return List.of();
    }

    List<UUID> collectionIds = enrollments.stream().map(e -> e.getCollection().getId()).toList();
    List<CollectionCourse> rows =
        collectionCourseRepository.findByCollection_IdInWithCourseStatus(
            collectionIds, PublishStatus.PUBLISHED);
    Map<UUID, List<CollectionCourse>> coursesByCollectionId = new HashMap<>();
    for (CollectionCourse row : rows) {
      coursesByCollectionId.computeIfAbsent(row.getCollection().getId(), ignored -> new java.util.ArrayList<>()).add(row);
    }

    List<UUID> allCourseIds = rows.stream().map(row -> row.getCourse().getId()).distinct().toList();
    Map<UUID, Integer> totalLessonsByCourseId = lessonCountByCourseId(allCourseIds);
    Map<UUID, Integer> completedLessonsByCourseId = completedLessonCountByCourseId(userId, allCourseIds);

    return enrollments.stream()
        .map(
            enrollment -> {
              Collection collection = enrollment.getCollection();
              List<CollectionCourse> collectionCourses =
                  coursesByCollectionId.getOrDefault(collection.getId(), List.of());
              int totalLessons = 0;
              int completedLessons = 0;
              for (CollectionCourse cc : collectionCourses) {
                UUID courseId = cc.getCourse().getId();
                totalLessons += totalLessonsByCourseId.getOrDefault(courseId, 0);
                completedLessons += completedLessonsByCourseId.getOrDefault(courseId, 0);
              }
              double progress = totalLessons == 0 ? 0.0 : (completedLessons * 100.0) / totalLessons;
              return StudentCollectionSummaryResponse.builder()
                  .collectionId(collection.getId())
                  .collectionName(collection.getTitle())
                  .collectionSlug(collection.getSlug())
                  .collectionType(collection.getType())
                  .thumbnailObjectKey(collection.getThumbnailObjectKey())
                  .progressPercentage(round2(progress))
                  .completedLessons(completedLessons)
                  .totalLessons(totalLessons)
                  .courseCount(collectionCourses.size())
                  .enrollmentStatus(enrollment.getStatus())
                  .enrolledAt(enrollment.getEnrolledAt())
                  .completedAt(enrollment.getCompletedAt())
                  .expiresAt(enrollment.getExpiresAt())
                  .build();
            })
        .toList();
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

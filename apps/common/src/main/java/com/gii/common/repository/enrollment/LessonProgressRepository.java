package com.gii.common.repository.enrollment;

import com.gii.common.entity.enrollment.LessonProgress;
import com.gii.common.entity.enrollment.LessonProgressId;
import com.gii.common.enums.PublishStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, LessonProgressId> {

  List<LessonProgress> findByUserIdAndLessonCourseId(UUID userId, UUID courseId);

  long countByUserIdAndLessonCourseIdAndCompletedAtIsNotNull(UUID userId, UUID courseId);

  long countByUserIdAndCompletedAtIsNotNull(UUID userId);

  @Query(
      """
        SELECT lp.lesson.course.id, COUNT(lp)
        FROM LessonProgress lp
        WHERE lp.user.id = :userId
        AND lp.lesson.course.id IN :courseIds
        AND lp.completedAt IS NOT NULL
        GROUP BY lp.lesson.course.id
      """)
  List<Object[]> countCompletedByUserIdAndCourseIds(
      @Param("userId") UUID userId, @Param("courseIds") List<UUID> courseIds);

  @Query(
      """
        SELECT lp.lesson.course.id, COUNT(lp)
        FROM LessonProgress lp
        WHERE lp.user.id = :userId
        AND lp.lesson.course.id IN :courseIds
        AND lp.lesson.status = :status
        AND lp.lesson.section.status = :status
        AND lp.completedAt IS NOT NULL
        GROUP BY lp.lesson.course.id
      """)
  List<Object[]> countCompletedPublishedByUserIdAndCourseIds(
      @Param("userId") UUID userId,
      @Param("courseIds") List<UUID> courseIds,
      @Param("status") PublishStatus status);

  @Query(
      """
        SELECT MAX(lp.updatedAt)
        FROM LessonProgress lp
        WHERE lp.user.id = :userId
      """)
  Instant findLatestActivityAtByUserId(@Param("userId") UUID userId);
}

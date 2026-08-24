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

  @Query(
      """
        SELECT lp FROM LessonProgress lp
        WHERE lp.enrollment.user.id = :userId
        AND lp.enrollment.course.id = :courseId
      """)
  List<LessonProgress> findByUserIdAndLessonCourseId(
      @Param("userId") UUID userId, @Param("courseId") UUID courseId);

  @Query(
      """
        SELECT COUNT(lp) FROM LessonProgress lp
        WHERE lp.enrollment.user.id = :userId
        AND lp.enrollment.course.id = :courseId
        AND lp.completedAt IS NOT NULL
      """)
  long countByUserIdAndLessonCourseIdAndCompletedAtIsNotNull(
      @Param("userId") UUID userId, @Param("courseId") UUID courseId);

  @Query(
      """
        SELECT COUNT(lp) FROM LessonProgress lp
        WHERE lp.enrollment.user.id = :userId
        AND lp.completedAt IS NOT NULL
      """)
  long countByUserIdAndCompletedAtIsNotNull(@Param("userId") UUID userId);

  @Query(
      """
        SELECT lp.enrollment.course.id, COUNT(lp)
        FROM LessonProgress lp
        WHERE lp.enrollment.user.id = :userId
        AND lp.enrollment.course.id IN :courseIds
        AND lp.completedAt IS NOT NULL
        GROUP BY lp.enrollment.course.id
      """)
  List<Object[]> countCompletedByUserIdAndCourseIds(
      @Param("userId") UUID userId, @Param("courseIds") List<UUID> courseIds);

  @Query(
      """
        SELECT lp.enrollment.course.id, COUNT(lp)
        FROM LessonProgress lp
        WHERE lp.enrollment.user.id = :userId
        AND lp.enrollment.course.id IN :courseIds
        AND lp.lesson.status = :status
        AND lp.lesson.section.status = :status
        AND lp.completedAt IS NOT NULL
        GROUP BY lp.enrollment.course.id
      """)
  List<Object[]> countCompletedPublishedByUserIdAndCourseIds(
      @Param("userId") UUID userId,
      @Param("courseIds") List<UUID> courseIds,
      @Param("status") PublishStatus status);

  @Query(
      """
        SELECT MAX(lp.updatedAt)
        FROM LessonProgress lp
        WHERE lp.enrollment.user.id = :userId
      """)
  Instant findLatestActivityAtByUserId(@Param("userId") UUID userId);
}

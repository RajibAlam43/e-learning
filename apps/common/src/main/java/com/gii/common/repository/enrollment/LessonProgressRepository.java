package com.gii.common.repository.enrollment;

import com.gii.common.entity.enrollment.LessonProgress;
import com.gii.common.entity.enrollment.LessonProgressId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, LessonProgressId> {

  List<LessonProgress> findByUserIdAndLessonCourseId(UUID userId, UUID courseId);

  long countByUserIdAndLessonCourseIdAndCompletedAtIsNotNull(UUID userId, UUID courseId);

  @Query(
      """
        SELECT lp.lesson.course.id, COUNT(lp)
        FROM LessonProgress lp
        WHERE lp.user.id = :userId
        AND lp.lesson.course.id IN :courseIds
        AND lp.completedAt IS NOT NULL
        GROUP BY lp.lesson.course.id
      """)
  List<Object[]> countCompletedByUserIdAndCourseIds(UUID userId, List<UUID> courseIds);
}

package com.gii.common.repository.course;

import com.gii.common.entity.course.Lesson;
import com.gii.common.enums.PublishStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {

  List<Lesson> findByCourseIdOrderByPositionAsc(UUID courseId);

  List<Lesson> findBySectionIdOrderByPositionAsc(UUID sectionId);

  @Query(
      """
        SELECT l FROM Lesson l
        LEFT JOIN FETCH l.primaryMediaAsset
        WHERE l.course.id = :courseId
        AND l.status = :status
        ORDER BY l.position ASC
      """)
  List<Lesson> findByCourseIdAndStatusWithMediaOrderByPositionAsc(
      @Param("courseId") UUID courseId, @Param("status") PublishStatus status);

  long countByCourseIdAndStatus(UUID courseId, PublishStatus status);

  @Query(
      """
        SELECT l.course.id, COUNT(l)
        FROM Lesson l
        WHERE l.course.id IN :courseIds
        AND l.status = :status
        GROUP BY l.course.id
      """)
  List<Object[]> countByCourseIdsAndStatus(
      @Param("courseIds") List<UUID> courseIds, @Param("status") PublishStatus status);
}

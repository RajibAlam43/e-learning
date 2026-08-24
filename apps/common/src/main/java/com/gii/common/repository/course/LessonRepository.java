package com.gii.common.repository.course;

import com.gii.common.entity.course.Lesson;
import com.gii.common.enums.PublishStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {

  @Query(
      """
        SELECT l FROM Lesson l
        WHERE l.section.templateVersion.id = (
          SELECT c.templateVersion.id FROM Course c WHERE c.id = :courseId
        )
        ORDER BY l.position ASC
      """)
  List<Lesson> findByCourseIdOrderByPositionAsc(@Param("courseId") UUID courseId);

  List<Lesson> findBySectionIdOrderByPositionAsc(UUID sectionId);

  @Query(
      """
        SELECT l FROM Lesson l
        LEFT JOIN FETCH l.primaryMediaAsset
        WHERE l.section.templateVersion.id = (
          SELECT c.templateVersion.id FROM Course c WHERE c.id = :courseId
        )
        AND l.status = :status
        ORDER BY l.position ASC
      """)
  List<Lesson> findByCourseIdAndStatusWithMediaOrderByPositionAsc(
      @Param("courseId") UUID courseId, @Param("status") PublishStatus status);

  @Query(
      """
        SELECT COUNT(l) FROM Lesson l
        WHERE l.section.templateVersion.id = (
          SELECT c.templateVersion.id FROM Course c WHERE c.id = :courseId
        )
        AND l.status = :status
      """)
  long countByCourseIdAndStatus(
      @Param("courseId") UUID courseId, @Param("status") PublishStatus status);

  @Query(
      """
        SELECT c.id, COUNT(l)
        FROM Course c, Lesson l
        WHERE c.id IN :courseIds
        AND l.section.templateVersion.id = c.templateVersion.id
        AND l.status = :status
        GROUP BY c.id
      """)
  List<Object[]> countByCourseIdsAndStatus(
      @Param("courseIds") List<UUID> courseIds, @Param("status") PublishStatus status);

  @Query(
      """
        SELECT c.id, COUNT(l)
        FROM Course c, Lesson l
        WHERE c.id IN :courseIds
        AND l.section.templateVersion.id = c.templateVersion.id
        AND l.status = :status
        AND l.section.status = :status
        GROUP BY c.id
      """)
  List<Object[]> countCompletableByCourseIdsAndStatus(
      @Param("courseIds") List<UUID> courseIds, @Param("status") PublishStatus status);
}

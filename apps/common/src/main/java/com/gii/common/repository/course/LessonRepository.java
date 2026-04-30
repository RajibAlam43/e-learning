package com.gii.common.repository.course;

import com.gii.common.entity.course.Lesson;
import com.gii.common.enums.PublishStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {

  Optional<Lesson> findByCourseIdAndSlug(UUID courseId, String slug);

  List<Lesson> findByCourseIdOrderByPositionAsc(UUID courseId);

  List<Lesson> findByCourseIdAndStatusOrderByPositionAsc(UUID courseId, PublishStatus status);

  @Query(
      """
        SELECT l FROM Lesson l
        LEFT JOIN FETCH l.primaryMediaAsset
        WHERE l.course.id = :courseId
        AND l.status = :status
        ORDER BY l.position ASC
      """)
  List<Lesson> findByCourseIdAndStatusWithMediaOrderByPositionAsc(
      UUID courseId, PublishStatus status);

  long countByCourseIdAndStatus(UUID courseId, PublishStatus status);
}

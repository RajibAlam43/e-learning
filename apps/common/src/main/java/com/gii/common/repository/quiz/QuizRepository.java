package com.gii.common.repository.quiz;

import com.gii.common.entity.quiz.Quiz;
import com.gii.common.enums.PublishStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {

  Optional<Quiz> findByIdAndStatus(UUID id, PublishStatus status);

  List<Quiz> findBySectionIdOrderByPositionAsc(UUID sectionId);

  List<Quiz> findByCourseIdAndStatusOrderByPositionAsc(UUID courseId, PublishStatus status);

  boolean existsBySectionIdAndPosition(UUID sectionId, Integer position);

  @Query(
      """
        SELECT q.course.id, COUNT(q)
        FROM Quiz q
        WHERE q.course.id IN :courseIds AND q.status = :status
        AND q.section.status = :status
        GROUP BY q.course.id
      """)
  List<Object[]> countByCourseIdsAndStatus(
      @Param("courseIds") List<UUID> courseIds, @Param("status") PublishStatus status);
}

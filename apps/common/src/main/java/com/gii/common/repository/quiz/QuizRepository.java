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

  @Query(
      """
        SELECT q FROM Quiz q
        WHERE q.section.templateVersion.id = (
          SELECT c.templateVersion.id FROM Course c WHERE c.id = :courseId
        )
        AND q.status = :status
        ORDER BY q.position ASC
      """)
  List<Quiz> findByCourseIdAndStatusOrderByPositionAsc(
      @Param("courseId") UUID courseId, @Param("status") PublishStatus status);

  boolean existsBySectionIdAndPosition(UUID sectionId, Integer position);

  @Query(
      """
        SELECT c.id, COUNT(q)
        FROM Course c, Quiz q
        WHERE c.id IN :courseIds
        AND q.section.templateVersion.id = c.templateVersion.id
        AND q.status = :status
        AND q.section.status = :status
        GROUP BY c.id
      """)
  List<Object[]> countByCourseIdsAndStatus(
      @Param("courseIds") List<UUID> courseIds, @Param("status") PublishStatus status);
}

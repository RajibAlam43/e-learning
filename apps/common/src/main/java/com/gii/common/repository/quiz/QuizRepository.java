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

  @Query(
      """
        SELECT q FROM Quiz q, SectionItem si
        WHERE q.section.id = :sectionId
        AND si.section.id = q.section.id
        AND si.itemType = com.gii.common.enums.SectionItemType.QUIZ
        AND si.itemId = q.id
        ORDER BY si.position ASC
      """)
  List<Quiz> findBySectionIdOrderByPositionAsc(@Param("sectionId") UUID sectionId);

  @Query(
      """
        SELECT q FROM Quiz q, SectionItem si
        WHERE q.section.templateVersion.id = (
          SELECT c.templateVersion.id FROM Course c WHERE c.id = :courseId
        )
        AND q.status = :status
        AND si.section.id = q.section.id
        AND si.itemType = com.gii.common.enums.SectionItemType.QUIZ
        AND si.itemId = q.id
        ORDER BY q.section.position ASC, si.position ASC
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
        AND q.section.isMandatory = true
        GROUP BY c.id
      """)
  List<Object[]> countByCourseIdsAndStatus(
      @Param("courseIds") List<UUID> courseIds, @Param("status") PublishStatus status);
}

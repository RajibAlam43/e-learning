package com.gii.common.repository.course;

import com.gii.common.entity.course.CourseSection;
import com.gii.common.enums.PublishStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseSectionRepository extends JpaRepository<CourseSection, UUID> {

  List<CourseSection> findByCourseIdOrderByPositionAsc(UUID courseId);

  List<CourseSection> findByCourseIdAndStatusOrderByPositionAsc(
      UUID courseId, PublishStatus status);

  @Query(
      """
        SELECT cs.course.id, COUNT(cs)
        FROM CourseSection cs
        WHERE cs.course.id IN :courseIds
        GROUP BY cs.course.id
      """)
  List<Object[]> countByCourseIds(@Param("courseIds") List<UUID> courseIds);

  @Query(
      """
        SELECT cs
        FROM CourseSection cs JOIN FETCH cs.course c
        WHERE cs.id = :sectionId
        AND c.id = :courseId
        AND EXISTS (
          SELECT 1 FROM CourseInstructor ci
          WHERE ci.course.id = c.id
          AND ci.instructor.id = :instructorId
        )
      """)
  Optional<CourseSection> findAssignedSectionForInstructor(
      @Param("courseId") UUID courseId,
      @Param("sectionId") UUID sectionId,
      @Param("instructorId") UUID instructorId);
}

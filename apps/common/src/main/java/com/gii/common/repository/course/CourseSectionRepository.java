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

  @Query(
      """
        SELECT cs FROM CourseSection cs
        WHERE cs.templateVersion.id = (
          SELECT c.templateVersion.id FROM Course c WHERE c.id = :courseId
        )
        ORDER BY cs.position ASC
      """)
  List<CourseSection> findByCourseIdOrderByPositionAsc(@Param("courseId") UUID courseId);

  @Query(
      """
        SELECT cs FROM CourseSection cs
        WHERE cs.templateVersion.id = (
          SELECT c.templateVersion.id FROM Course c WHERE c.id = :courseId
        )
        AND cs.status = :status
        ORDER BY cs.position ASC
      """)
  List<CourseSection> findByCourseIdAndStatusOrderByPositionAsc(
      @Param("courseId") UUID courseId, @Param("status") PublishStatus status);

  @Query(
      """
        SELECT c.id, COUNT(cs)
        FROM Course c, CourseSection cs
        WHERE c.id IN :courseIds
        AND cs.templateVersion.id = c.templateVersion.id
        GROUP BY c.id
      """)
  List<Object[]> countByCourseIds(@Param("courseIds") List<UUID> courseIds);

  @Query(
      """
        SELECT cs
        FROM CourseSection cs, Course c
        WHERE cs.id = :sectionId
        AND cs.templateVersion.id = c.templateVersion.id
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

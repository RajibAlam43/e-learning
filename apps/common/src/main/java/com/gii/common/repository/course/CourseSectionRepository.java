package com.gii.common.repository.course;

import com.gii.common.entity.course.CourseSection;
import com.gii.common.enums.PublishStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
  List<Object[]> countByCourseIds(List<UUID> courseIds);
}

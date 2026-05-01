package com.gii.common.repository.course;

import com.gii.common.entity.course.CourseInstructor;
import com.gii.common.entity.course.CourseInstructorId;
import com.gii.common.enums.PublishStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CourseInstructorRepository
    extends JpaRepository<CourseInstructor, CourseInstructorId> {

  @Query(
      "SELECT ci FROM CourseInstructor ci JOIN FETCH ci.instructor WHERE ci.course.id = :courseId")
  List<CourseInstructor> findByCourseId(UUID courseId);

  @Query(
      """
        SELECT ci
        FROM CourseInstructor ci JOIN FETCH ci.instructor
        WHERE ci.course.id IN :courseIds
      """)
  List<CourseInstructor> findByCourseIds(List<UUID> courseIds);

  @Query("SELECT ci FROM CourseInstructor ci WHERE ci.instructor.id = :instructorUserId")
  List<CourseInstructor> findByInstructorId(UUID instructorUserId);

  @Query(
      """
        SELECT ci.instructor.id, COUNT(ci)
        FROM CourseInstructor ci
        WHERE ci.instructor.id IN :instructorUserIds
        GROUP BY ci.instructor.id
      """)
  List<Object[]> countByInstructorIds(List<UUID> instructorUserIds);

  @Query(
      """
        SELECT ci.instructor.id, COUNT(ci)
        FROM CourseInstructor ci
        WHERE ci.instructor.id IN :instructorUserIds
        AND ci.course.status = :status
        GROUP BY ci.instructor.id
      """)
  List<Object[]> countByInstructorIdsAndCourseStatus(
      List<UUID> instructorUserIds, PublishStatus status);

  @Query(
      """
        SELECT ci
        FROM CourseInstructor ci JOIN FETCH ci.course c
        WHERE ci.instructor.id = :instructorUserId
        AND c.status = :status
      """)
  List<CourseInstructor> findByInstructorIdAndCourseStatus(
      UUID instructorUserId, PublishStatus status);

  @Query(
      """
        SELECT CASE WHEN COUNT(ci) > 0 THEN true ELSE false END
        FROM CourseInstructor ci
        WHERE ci.course.id = :courseId
        AND ci.instructor.id = :instructorUserId
      """)
  boolean existsByCourseIdAndInstructorId(UUID courseId, UUID instructorUserId);
}

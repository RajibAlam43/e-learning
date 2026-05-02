package com.gii.common.repository.course;

import com.gii.common.entity.course.CourseInstructor;
import com.gii.common.entity.course.CourseInstructorId;
import com.gii.common.enums.PublishStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseInstructorRepository
    extends JpaRepository<CourseInstructor, CourseInstructorId> {

  @Query(
      "SELECT ci FROM CourseInstructor ci JOIN FETCH ci.instructor WHERE ci.course.id = :courseId")
  List<CourseInstructor> findByCourseId(@Param("courseId") UUID courseId);

  @Query(
      """
        SELECT ci
        FROM CourseInstructor ci JOIN FETCH ci.instructor
        WHERE ci.course.id IN :courseIds
      """)
  List<CourseInstructor> findByCourseIds(@Param("courseIds") List<UUID> courseIds);

  @Query("SELECT ci FROM CourseInstructor ci WHERE ci.instructor.id = :instructorUserId")
  List<CourseInstructor> findByInstructorId(@Param("instructorUserId") UUID instructorUserId);

  @Query(
      """
        SELECT ci.instructor.id, COUNT(ci)
        FROM CourseInstructor ci
        WHERE ci.instructor.id IN :instructorUserIds
        GROUP BY ci.instructor.id
      """)
  List<Object[]> countByInstructorIds(@Param("instructorUserIds") List<UUID> instructorUserIds);

  @Query(
      """
        SELECT ci.instructor.id, COUNT(ci)
        FROM CourseInstructor ci
        WHERE ci.instructor.id IN :instructorUserIds
        AND ci.course.status = :status
        GROUP BY ci.instructor.id
      """)
  List<Object[]> countByInstructorIdsAndCourseStatus(
      @Param("instructorUserIds") List<UUID> instructorUserIds,
      @Param("status") PublishStatus status);

  @Query(
      """
        SELECT ci
        FROM CourseInstructor ci JOIN FETCH ci.course c
        WHERE ci.instructor.id = :instructorUserId
        AND c.status = :status
      """)
  List<CourseInstructor> findByInstructorIdAndCourseStatus(
      @Param("instructorUserId") UUID instructorUserId, @Param("status") PublishStatus status);

  @Query(
      """
        SELECT CASE WHEN COUNT(ci) > 0 THEN true ELSE false END
        FROM CourseInstructor ci
        WHERE ci.course.id = :courseId
        AND ci.instructor.id = :instructorUserId
      """)
  boolean existsByCourseIdAndInstructorId(
      @Param("courseId") UUID courseId, @Param("instructorUserId") UUID instructorUserId);
}

package com.gii.common.repository.enrollment;

import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.enums.EnrollmentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

  Optional<Enrollment> findByUserIdAndCourseId(UUID userId, UUID courseId);

  Optional<Enrollment> findByUserIdAndCourseIdAndStatus(
      UUID userId, UUID courseId, EnrollmentStatus status);

  boolean existsByUserIdAndCourseIdAndStatus(UUID userId, UUID courseId, EnrollmentStatus status);

  List<Enrollment> findByUserIdAndStatus(UUID userId, EnrollmentStatus status);

  long countByUserIdAndStatus(UUID userId, EnrollmentStatus status);

  long countByUserIdAndStatusAndCompletedAtIsNotNull(UUID userId, EnrollmentStatus status);

  @Query(
      """
        SELECT e.course.id, COUNT(e)
        FROM Enrollment e
        WHERE e.course.id IN :courseIds
        AND e.status = :status
        GROUP BY e.course.id
      """)
  List<Object[]> countByCourseIdsAndStatus(List<UUID> courseIds, EnrollmentStatus status);

  @Query(
      """
        SELECT e.course.id, COUNT(e)
        FROM Enrollment e
        WHERE e.course.id IN :courseIds
        AND e.status = :status
        AND e.completedAt IS NOT NULL
        GROUP BY e.course.id
      """)
  List<Object[]> countCompletedByCourseIdsAndStatus(List<UUID> courseIds, EnrollmentStatus status);
}

package com.gii.common.repository.enrollment;

import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.enums.EnrollmentStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

  Optional<Enrollment> findByUserIdAndCourseId(UUID userId, UUID courseId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
        SELECT e
        FROM Enrollment e
        WHERE e.user.id = :userId
        AND e.course.id = :courseId
      """)
  Optional<Enrollment> findByUserIdAndCourseIdForUpdate(
      @Param("userId") UUID userId, @Param("courseId") UUID courseId);

  Optional<Enrollment> findByUserIdAndCourseIdAndStatus(
      UUID userId, UUID courseId, EnrollmentStatus status);

  @Query(
      """
        SELECT e
        FROM Enrollment e
        WHERE e.user.id = :userId
        AND e.course.template.id = :templateId
        AND e.status = :status
        ORDER BY e.enrolledAt DESC
      """)
  List<Enrollment> findByUserIdAndTemplateIdAndStatus(
      @Param("userId") UUID userId,
      @Param("templateId") UUID templateId,
      @Param("status") EnrollmentStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT e FROM Enrollment e WHERE e.sourceOrderItem.order.id = :orderId")
  List<Enrollment> findBySourceOrderIdForUpdate(@Param("orderId") UUID orderId);

  boolean existsByUserIdAndCourseIdAndStatus(UUID userId, UUID courseId, EnrollmentStatus status);

  boolean existsByCourseId(UUID courseId);

  List<Enrollment> findByUserIdAndStatus(UUID userId, EnrollmentStatus status);

  long countByUserIdAndStatus(UUID userId, EnrollmentStatus status);

  long countByUserIdAndStatusAndCompletedAtIsNotNull(UUID userId, EnrollmentStatus status);

  @Query(
      """
        SELECT COUNT(e)
        FROM Enrollment e
        WHERE e.course.id = :courseId
        AND e.status = :status
        AND (e.expiresAt IS NULL OR e.expiresAt > :now)
      """)
  long countAvailableSeatsInUse(
      @Param("courseId") UUID courseId,
      @Param("status") EnrollmentStatus status,
      @Param("now") java.time.Instant now);

  List<Enrollment> findByCourseIdAndStatus(UUID courseId, EnrollmentStatus status);

  @Query(
      """
        SELECT e.course.id, COUNT(e)
        FROM Enrollment e
        WHERE e.course.id IN :courseIds
        AND e.status = :status
        GROUP BY e.course.id
      """)
  List<Object[]> countByCourseIdsAndStatus(
      @Param("courseIds") List<UUID> courseIds, @Param("status") EnrollmentStatus status);

  @Query(
      """
        SELECT e.course.id, COUNT(e)
        FROM Enrollment e
        WHERE e.course.id IN :courseIds
        AND e.status = :status
        AND e.completedAt IS NOT NULL
        GROUP BY e.course.id
      """)
  List<Object[]> countCompletedByCourseIdsAndStatus(
      @Param("courseIds") List<UUID> courseIds, @Param("status") EnrollmentStatus status);

  @Query(
      """
        SELECT e.course.id
        FROM Enrollment e
        WHERE e.user.id = :userId
        AND e.course.id IN :courseIds
        AND e.completedAt IS NOT NULL
      """)
  List<UUID> findCompletedCourseIds(
      @Param("userId") UUID userId, @Param("courseIds") List<UUID> courseIds);
}

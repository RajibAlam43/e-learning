package com.gii.common.repository.enrollment;

import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.enums.EnrollmentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

  Optional<Enrollment> findByUserIdAndCourseId(UUID userId, UUID courseId);

  boolean existsByUserIdAndCourseIdAndStatus(UUID userId, UUID courseId, EnrollmentStatus status);

  List<Enrollment> findByUserIdAndStatus(UUID userId, EnrollmentStatus status);

  List<Enrollment> findByCourseIdAndStatus(UUID courseId, EnrollmentStatus status);

  long countByCourseIdAndStatus(UUID courseId, EnrollmentStatus status);

  long countByCourseIdAndStatusAndCompletedAtIsNotNull(UUID courseId, EnrollmentStatus status);

  long countByUserIdAndStatus(UUID userId, EnrollmentStatus status);

  long countByUserIdAndStatusAndCompletedAtIsNotNull(UUID userId, EnrollmentStatus status);
}

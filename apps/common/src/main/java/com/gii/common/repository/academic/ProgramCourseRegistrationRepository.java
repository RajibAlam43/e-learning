package com.gii.common.repository.academic;

import com.gii.common.entity.academic.ProgramCourseRegistration;
import com.gii.common.enums.ProgramRegistrationStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProgramCourseRegistrationRepository
    extends JpaRepository<ProgramCourseRegistration, UUID> {
  List<ProgramCourseRegistration> findByProgramEnrollmentUserIdOrderByCreatedAtDesc(
      UUID userId, org.springframework.data.domain.Pageable pageable);

  List<ProgramCourseRegistration> findByProgramEnrollmentIdAndStatus(
      UUID programEnrollmentId, ProgramRegistrationStatus status);

  List<ProgramCourseRegistration> findByProgramEnrollmentIdOrderByCreatedAtDesc(
      UUID programEnrollmentId);

  List<ProgramCourseRegistration> findByEnrollmentIdAndStatus(
      UUID enrollmentId, ProgramRegistrationStatus status);

  @Query(
      """
      select r from ProgramCourseRegistration r
      join fetch r.programEnrollment p join fetch r.enrollment e
      where p.user.id=:userId order by r.createdAt desc
      """)
  List<ProgramCourseRegistration> findByUser(@Param("userId") UUID userId);
}

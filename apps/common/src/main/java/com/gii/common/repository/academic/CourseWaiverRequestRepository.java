package com.gii.common.repository.academic;

import com.gii.common.entity.academic.CourseWaiverRequest;
import com.gii.common.enums.WaiverStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseWaiverRequestRepository extends JpaRepository<CourseWaiverRequest, UUID> {
  List<CourseWaiverRequest> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable page);

  List<CourseWaiverRequest> findByStatusOrderByCreatedAtAsc(WaiverStatus status, Pageable page);

  boolean existsByUserIdAndCourseTemplateIdAndStatus(
      UUID userId, UUID templateId, WaiverStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select w from CourseWaiverRequest w where w.id=:id")
  Optional<CourseWaiverRequest> findForUpdate(@Param("id") UUID id);
}

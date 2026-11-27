package com.gii.common.repository.academic;

import com.gii.common.entity.academic.ManualEnrollmentAudit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ManualEnrollmentAuditRepository
    extends JpaRepository<ManualEnrollmentAudit, UUID> {
  Optional<ManualEnrollmentAudit> findByRequestId(UUID requestId);

  List<ManualEnrollmentAudit> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}

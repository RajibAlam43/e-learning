package com.gii.common.repository.audit;

import com.gii.common.entity.audit.AuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

  List<AuditLog> findByActorUserIdOrderByCreatedAtDesc(UUID actorUserId);

  List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
      String entityType, String entityId);
}

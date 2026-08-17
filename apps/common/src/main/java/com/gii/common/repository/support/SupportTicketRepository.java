package com.gii.common.repository.support;

import com.gii.common.entity.support.SupportTicket;
import com.gii.common.enums.SupportTicketStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {

  List<SupportTicket> findByStatusOrderByCreatedAtDesc(SupportTicketStatus status);

  List<SupportTicket> findByUserIdOrderByCreatedAtDesc(UUID userId);

  boolean existsByRateLimitKeyHashAndCreatedAtAfter(String rateLimitKeyHash, Instant createdAfter);

  @Query(value = "SELECT pg_advisory_xact_lock(:lockKey)", nativeQuery = true)
  void lockRateLimitKey(@Param("lockKey") long lockKey);
}

package com.gii.common.repository.support;

import com.gii.common.entity.support.SupportTicket;
import com.gii.common.enums.SupportTicketStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {

  List<SupportTicket> findByStatusOrderByCreatedAtDesc(SupportTicketStatus status);

  List<SupportTicket> findByUserIdOrderByCreatedAtDesc(UUID userId);
}

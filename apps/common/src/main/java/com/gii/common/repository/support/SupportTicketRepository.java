package com.gii.common.repository.support;

import com.gii.common.entity.support.SupportTicket;
import com.gii.common.enums.SupportTicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {

    List<SupportTicket> findByStatusOrderByCreatedAtDesc(SupportTicketStatus status);

    List<SupportTicket> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
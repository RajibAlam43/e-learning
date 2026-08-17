package com.gii.api.model.response.admin;

import com.gii.common.enums.SupportTicketStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminSupportTicketResponse(
    UUID ticketId,
    UUID userId,
    String name,
    String email,
    String phone,
    String subject,
    String message,
    SupportTicketStatus status,
    Instant closedAt,
    Instant createdAt,
    Instant updatedAt) {}

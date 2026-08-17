package com.gii.api.model.response;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record SupportTicketCreatedResponse(UUID ticketId, String status, Instant createdAt) {}

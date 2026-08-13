package com.gii.api.model.request.admin;

import com.gii.common.enums.SupportTicketStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateSupportTicketRequest(@NotNull SupportTicketStatus status) {}

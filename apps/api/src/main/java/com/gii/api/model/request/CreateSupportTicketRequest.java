package com.gii.api.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CreateSupportTicketRequest(
    String name, // Optional: if user is logged in, can be autofilled
    @Email String email, // Required for contact
    String phone, // Optional: additional contact method
    @NotBlank String subject, // Ticket subject
    @NotBlank String message // Detailed message
    ) {}

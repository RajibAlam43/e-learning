package com.gii.api.model.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record RegisterRequest(
    @NotBlank String fullName,
    @Email String email, // Optional, but at least one of email/phone required
    @Pattern(regexp = "^[0-9+\\-\\s()]+$")
        String phoneNumber, // Optional, but at least one of email/phone required
    @NotBlank String password) {}

package com.gii.api.model.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record RegisterRequest(
        @NotBlank String fullName,
        @Email String email,  // Optional, but at least one of email/phone required
        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$") String phoneNumber,  // Optional, E.164 format, but at least one of email/phone required
        String phoneCountryCode,  // e.g., "+880" for BD
        @NotBlank String password
) {}
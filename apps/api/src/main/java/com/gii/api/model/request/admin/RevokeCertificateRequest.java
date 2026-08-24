package com.gii.api.model.request.admin;

import jakarta.validation.constraints.Size;

public record RevokeCertificateRequest(
    @Size(max = 1000, message = "Revocation reason must not exceed 1000 characters")
        String reason) {}

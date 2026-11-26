package com.gii.api.model.request.admin;

import com.gii.common.enums.WaiverStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ReviewWaiverRequest(
    @NotNull WaiverStatus decision,
    @NotBlank @Size(max = 5000) String reason,
    @NotNull @Positive Integer expectedCredits,
    UUID semesterId) {}

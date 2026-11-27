package com.gii.api.model.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WithdrawProgramRegistrationRequest(@NotBlank @Size(max = 5000) String reason) {}

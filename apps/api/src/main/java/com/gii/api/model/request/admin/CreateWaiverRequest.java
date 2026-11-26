package com.gii.api.model.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateWaiverRequest(
    @NotNull UUID courseTemplateId,
    @NotBlank @Size(max = 5000) String reason,
    @Size(max = 300) String evidenceObjectKey) {}

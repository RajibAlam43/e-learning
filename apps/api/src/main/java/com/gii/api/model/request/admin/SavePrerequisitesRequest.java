package com.gii.api.model.request.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record SavePrerequisitesRequest(
    @NotNull @Size(max = 100) List<@NotNull UUID> courseTemplateIds) {}

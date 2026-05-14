package com.gii.api.model.request.admin;

import com.gii.common.enums.LiveClassProvider;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CreateLiveClassRequest(
    @NotNull UUID sectionId,
    @NotBlank String title,
    String description,
    @NotNull @Future Instant startsAt,
    @NotNull @Future Instant endsAt,
    @NotNull LiveClassProvider provider,
    @NotNull @Positive Integer maxCapacity) {}

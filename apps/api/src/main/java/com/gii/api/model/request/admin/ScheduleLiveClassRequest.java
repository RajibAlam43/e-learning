package com.gii.api.model.request.admin;

import com.gii.common.enums.LiveClassProvider;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import lombok.Builder;

@Builder
public record ScheduleLiveClassRequest(
    @NotNull @Future Instant startsAt,
    @NotNull @Future Instant endsAt,
    @NotNull LiveClassProvider provider,
    @NotNull @Positive Integer maxCapacity) {}

package com.gii.api.model.request.admin;

import com.gii.common.enums.LiveClassProvider;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Builder;

@Builder
public record ScheduleExternalLiveClassRequest(
    @NotNull @Future Instant startsAt,
    @NotNull @Future Instant endsAt,
    @NotNull LiveClassProvider provider,
    @NotBlank @Size(max = 2048) String participantJoinUrl,
    @Size(max = 2048) String hostStartUrl,
    @Size(max = 255) String meetingId,
    @NotNull @Positive Integer maxCapacity) {}

package com.gii.api.model.request.instructor;

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
    UUID liveClassItemId, // Existing curriculum slot; required for instructors
    UUID sectionId, // Required only by the legacy admin create flow
    @Positive Integer position, // Admin-only legacy field; instructors cannot reorder curriculum
    @NotBlank String title,
    String titleEn,
    String description, // Optional
    String descriptionEn,
    @NotNull @Future Instant startsAt,
    @NotNull @Future Instant endsAt,
    @NotNull LiveClassProvider provider,
    @NotNull @Positive Integer maxCapacity) {}

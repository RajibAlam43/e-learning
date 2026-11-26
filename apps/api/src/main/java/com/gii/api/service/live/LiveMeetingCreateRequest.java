package com.gii.api.service.live;

import com.gii.common.enums.LiveClassProvider;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record LiveMeetingCreateRequest(
    LiveClassProvider provider,
    String title,
    String description,
    Instant startsAt,
    Instant endsAt,
    Integer maxCapacity,
    List<String> coHostEmails,
    UUID primaryTeacherId,
    List<String> calendarGuestEmails) {}

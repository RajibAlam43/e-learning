package com.gii.api.service.live;

import java.util.Map;
import lombok.Builder;

@Builder
public record LiveMeetingCreateResult(
    String meetingId,
    String hostStartUrl,
    String participantJoinUrl,
    Map<String, Object> metadata) {}

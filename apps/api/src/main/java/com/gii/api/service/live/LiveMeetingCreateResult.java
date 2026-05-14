package com.gii.api.service.live;

import lombok.Builder;

@Builder
public record LiveMeetingCreateResult(String meetingId, String hostStartUrl, String participantJoinUrl) {}

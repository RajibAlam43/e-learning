package com.gii.api.service.live;

import com.gii.common.enums.LiveClassProvider;
import java.util.Map;
import lombok.Builder;

@Builder
public record LiveMeetingCancelRequest(
    LiveClassProvider provider, String providerMeetingId, Map<String, Object> metadata) {}

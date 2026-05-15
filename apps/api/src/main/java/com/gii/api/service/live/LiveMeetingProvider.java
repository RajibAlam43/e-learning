package com.gii.api.service.live;

import com.gii.common.enums.LiveClassProvider;

public interface LiveMeetingProvider {
  LiveClassProvider provider();

  LiveMeetingCreateResult create(LiveMeetingCreateRequest request);

  void update(LiveMeetingUpdateRequest request);

  void cancel(LiveMeetingCancelRequest request);
}

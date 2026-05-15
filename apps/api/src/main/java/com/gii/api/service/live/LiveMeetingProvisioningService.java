package com.gii.api.service.live;

import com.gii.common.enums.LiveClassProvider;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class LiveMeetingProvisioningService {
  private final List<LiveMeetingProvider> providers;

  public LiveMeetingCreateResult createMeeting(LiveMeetingCreateRequest request) {
    LiveMeetingProvider provider = providerIndex().get(request.provider());
    if (provider == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Unsupported live class provider: " + request.provider());
    }
    return provider.create(request);
  }

  public void updateMeeting(LiveMeetingUpdateRequest request) {
    LiveMeetingProvider provider = providerIndex().get(request.provider());
    if (provider == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Unsupported live class provider: " + request.provider());
    }
    provider.update(request);
  }

  public void cancelMeeting(LiveMeetingCancelRequest request) {
    LiveMeetingProvider provider = providerIndex().get(request.provider());
    if (provider == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Unsupported live class provider: " + request.provider());
    }
    provider.cancel(request);
  }

  private Map<LiveClassProvider, LiveMeetingProvider> providerIndex() {
    Map<LiveClassProvider, LiveMeetingProvider> index = new EnumMap<>(LiveClassProvider.class);
    for (LiveMeetingProvider provider : providers) {
      index.put(provider.provider(), provider);
    }
    return index;
  }
}

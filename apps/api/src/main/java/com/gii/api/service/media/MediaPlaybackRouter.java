package com.gii.api.service.media;

import com.gii.api.model.response.MediaPlaybackResponse;
import com.gii.common.entity.course.MediaAsset;
import com.gii.common.enums.MediaProvider;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MediaPlaybackRouter {

  private final Map<MediaProvider, MediaProviderService> services;

  public MediaPlaybackRouter(List<MediaProviderService> providerServices) {
    this.services = new EnumMap<>(MediaProvider.class);

    for (MediaProviderService service : providerServices) {
      services.put(service.provider(), service);
    }
  }

  public MediaPlaybackResponse getPlayback(MediaAsset mediaAsset) {
    MediaProviderService service = services.get(mediaAsset.getProvider());

    if (service == null) {
      throw new RuntimeException("Unsupported media provider: " + mediaAsset.getProvider());
    }

    return service.getPlayback(mediaAsset);
  }
}

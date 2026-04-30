package com.gii.api.service.media;

import com.gii.api.model.response.MediaPlaybackResponse;
import com.gii.common.entity.course.MediaAsset;
import com.gii.common.enums.MediaProvider;

public interface MediaProviderService {
  MediaProvider provider();

  MediaPlaybackResponse getPlayback(MediaAsset mediaAsset);
}

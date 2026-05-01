package com.gii.api.service.media;

import com.gii.api.model.response.MediaPlaybackResponse;
import com.gii.common.entity.course.MediaAsset;
import com.gii.common.enums.MediaProvider;

public class YoutubeService implements MediaProviderService {

  @Override
  public MediaProvider provider() {
    return MediaProvider.YOUTUBE;
  }

  @Override
  public MediaPlaybackResponse getPlayback(MediaAsset mediaAsset) {
    String embedUrl = "https://www.youtube.com/embed/" + mediaAsset.getProviderAssetId();

    return new MediaPlaybackResponse(
        MediaProvider.YOUTUBE,
        com.gii.common.enums.PlaybackMode.IFRAME,
        embedUrl,
        null,
        embedUrl,
        null,
        null,
        null);
  }
}

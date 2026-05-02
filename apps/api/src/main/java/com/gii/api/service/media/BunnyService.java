package com.gii.api.service.media;

import com.gii.api.model.response.MediaPlaybackResponse;
import com.gii.api.service.util.CryptoOperationException;
import com.gii.common.entity.course.MediaAsset;
import com.gii.common.enums.MediaProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BunnyService implements MediaProviderService {

  @Value("${bunny.token-security-key}")
  private String tokenSecurityKey;

  @Value("${bunny.playback-token-ttl-s:3600}")
  private long playbackTokenTtlSeconds;

  @Override
  public MediaProvider provider() {
    return MediaProvider.BUNNY;
  }

  @Override
  public MediaPlaybackResponse getPlayback(MediaAsset mediaAsset) {

    long expires = Instant.now().plusSeconds(playbackTokenTtlSeconds).getEpochSecond();

    String videoId = mediaAsset.getProviderAssetId();
    String libraryId = mediaAsset.getProviderLibraryId();

    String token = sha256Hex(tokenSecurityKey + videoId + expires);

    // iframe embed
    String embedUrl =
        "https://iframe.mediadelivery.net/embed/"
            + libraryId
            + "/"
            + videoId
            + "?token="
            + token
            + "&expires="
            + expires;

    // direct HLS
    String hlsUrl =
        "https://vz-"
            + libraryId
            + ".b-cdn.net/"
            + videoId
            + "/playlist.m3u8"
            + "?token="
            + token
            + "&expires="
            + expires;

    return new MediaPlaybackResponse(
        MediaProvider.BUNNY,
        com.gii.common.enums.PlaybackMode.HLS,
        embedUrl,
        hlsUrl,
        hlsUrl,
        videoId,
        token,
        Instant.ofEpochSecond(expires));
  }

  private String sha256Hex(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));

      StringBuilder hex = new StringBuilder();

      for (byte b : hash) {
        hex.append(String.format("%02x", b));
      }

      return hex.toString();
    } catch (Exception e) {
      throw new CryptoOperationException("Failed to generate Bunny token", e);
    }
  }
}

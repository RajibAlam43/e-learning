package com.gii.api.service.media;

import com.gii.api.model.response.RecordingResponse.Playback;
import com.gii.common.entity.live.LiveClassRecording;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class RecordingPlaybackService {
  @Value("${recordings.cloudfront.base-url:}")
  private String baseUrl;

  @Value("${recordings.cloudfront.key-pair-id:}")
  private String keyPairId;

  @Value("${recordings.cloudfront.private-key:}")
  private String privateKey;

  @Value("${recordings.cloudfront.cookie-domain:}")
  private String cookieDomain;

  public String fileUrl(LiveClassRecording recording) {
    String url = baseUrl.replaceAll("/+$", "") + "/" + recording.getObjectPrefix() + "playback.mp4";
    if (!url.startsWith("https://") || keyPairId.isBlank() || privateKey.isBlank()) {
      throw new IllegalStateException(
          "Configure CloudFront before publishing recording playback links");
    }
    long expires =
        Instant.now()
            .plusSeconds(
                Math.min(
                    36000, Math.max(7200, (long) Math.ceil(recording.getDurationSeconds()) + 3600)))
            .getEpochSecond();
    String policy =
        "{\"Statement\":[{\"Resource\":\""
            + url
            + "\",\"Condition\":{\"DateLessThan\":{\"AWS:EpochTime\":"
            + expires
            + "}}}]}";
    return url
        + "?Policy="
        + encode(policy.getBytes(StandardCharsets.UTF_8))
        + "&Signature="
        + signature(policy)
        + "&Key-Pair-Id="
        + keyPairId;
  }

  private String signature(String policy) {
    try {
      String pem =
          privateKey.contains("BEGIN")
              ? privateKey
              : new String(Base64.getDecoder().decode(privateKey), StandardCharsets.UTF_8);
      byte[] der =
          Base64.getMimeDecoder()
              .decode(
                  pem.replace("-----BEGIN PRIVATE KEY-----", "")
                      .replace("-----END PRIVATE KEY-----", ""));
      Signature signer = Signature.getInstance("SHA1withRSA");
      signer.initSign(KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der)));
      signer.update(policy.getBytes(StandardCharsets.UTF_8));
      return encode(signer.sign());
    } catch (java.security.GeneralSecurityException | IllegalArgumentException error) {
      throw new IllegalStateException(
          "Recording playback signing is not configured correctly", error);
    }
  }

  public Playback issue(
      LiveClassRecording recording, HttpServletRequest request, HttpServletResponse response) {
    String host = URI.create(baseUrl).getHost();
    String domain = cookieDomain.replaceFirst("^\\.", "");
    if (!baseUrl.startsWith("https://")
        || domain.isBlank()
        || keyPairId.isBlank()
        || privateKey.isBlank()
        || host == null
        || !matchesDomain(host, domain)
        || !matchesDomain(request.getServerName(), domain)) {
      throw new IllegalStateException(
          "Configure a shared API/media cookie domain and CloudFront signing key");
    }
    String path = "/" + recording.getObjectPrefix() + "hls/";
    Instant expires =
        Instant.now()
            .plusSeconds(
                Math.min(
                    36000,
                    Math.max(7200, (long) Math.ceil(recording.getDurationSeconds()) + 3600)));
    String policy =
        "{\"Statement\":[{\"Resource\":\""
            + baseUrl.replaceAll("/+$", "")
            + path
            + "*\",\"Condition\":{\"DateLessThan\":{\"AWS:EpochTime\":"
            + expires.getEpochSecond()
            + "}}}]}";
    addCookie(
        response,
        "CloudFront-Policy",
        encode(policy.getBytes(StandardCharsets.UTF_8)),
        path,
        expires);
    addCookie(response, "CloudFront-Signature", signature(policy), path, expires);
    addCookie(response, "CloudFront-Key-Pair-Id", keyPairId, path, expires);
    response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    return new Playback(
        baseUrl.replaceAll("/+$", "") + path + "index.m3u8", fileUrl(recording), expires);
  }

  private boolean matchesDomain(String host, String domain) {
    return host.equals(domain) || host.endsWith("." + domain);
  }

  private String encode(byte[] bytes) {
    return Base64.getEncoder()
        .encodeToString(bytes)
        .replace('+', '-')
        .replace('=', '_')
        .replace('/', '~');
  }

  private void addCookie(
      HttpServletResponse response, String name, String value, String path, Instant expires) {
    response.addHeader(
        HttpHeaders.SET_COOKIE,
        ResponseCookie.from(name, value)
            .domain(cookieDomain)
            .path(path)
            .secure(true)
            .httpOnly(true)
            .sameSite("Lax")
            .maxAge(Duration.between(Instant.now(), expires))
            .build()
            .toString());
  }
}

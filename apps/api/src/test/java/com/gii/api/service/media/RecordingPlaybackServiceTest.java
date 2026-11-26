package com.gii.api.service.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gii.common.entity.live.LiveClassRecording;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

class RecordingPlaybackServiceTest {
  private final RecordingPlaybackService service = new RecordingPlaybackService();
  private final LiveClassRecording recording =
      LiveClassRecording.builder()
          .objectPrefix("recordings/class/recording/")
          .durationSeconds(3600.0)
          .build();
  private KeyPair keys;

  @BeforeEach
  void setup() throws Exception {
    keys = KeyPairGenerator.getInstance("RSA").generateKeyPair();
    ReflectionTestUtils.setField(service, "baseUrl", "https://media.example.com");
    ReflectionTestUtils.setField(service, "cookieDomain", "example.com");
    ReflectionTestUtils.setField(service, "keyPairId", "key-id");
    ReflectionTestUtils.setField(
        service,
        "privateKey",
        Base64.getEncoder()
            .encodeToString(
                ("-----BEGIN PRIVATE KEY-----\n"
                        + Base64.getEncoder().encodeToString(keys.getPrivate().getEncoded())
                        + "\n-----END PRIVATE KEY-----")
                    .getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void signsExactMp4ResourceWithVerifiableSignature() throws Exception {
    URI url = URI.create(service.fileUrl(recording));
    Map<String, String> query =
        java.util.Arrays.stream(url.getRawQuery().split("&"))
            .map(p -> p.split("=", 2))
            .collect(Collectors.toMap(p -> p[0], p -> p[1]));
    byte[] policy = decode(query.get("Policy"));
    assertThat(new String(policy, StandardCharsets.UTF_8))
        .contains("https://media.example.com/recordings/class/recording/playback.mp4")
        .doesNotContain("*");
    Signature verifier = Signature.getInstance("SHA1withRSA");
    verifier.initVerify(keys.getPublic());
    verifier.update(policy);
    assertThat(verifier.verify(decode(query.get("Signature")))).isTrue();
  }

  @Test
  void hlsCookiesAreSecureHttpOnlyAndLimitedToThisRecording() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setServerName("api.example.com");
    MockHttpServletResponse response = new MockHttpServletResponse();
    var result = service.issue(recording, request, response);
    assertThat(result.url()).endsWith("/hls/index.m3u8");
    assertThat(result.fileUrl()).contains("/playback.mp4?Policy=");
    assertThat(response.getHeaders("Set-Cookie"))
        .hasSize(3)
        .allSatisfy(cookie -> {
          assertThat(cookie)
              .contains(
                  "Secure", "HttpOnly", "SameSite=Lax", "Path=/recordings/class/recording/hls/");
        });
    assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
  }

  @Test
  void rejectsCookieDomainsThatCannotAuthorizeTheMediaHost() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setServerName("api.unrelated.com");
    assertThatThrownBy(() -> service.issue(recording, request, new MockHttpServletResponse()))
        .isInstanceOf(IllegalStateException.class);
  }

  private byte[] decode(String value) {
    return Base64.getDecoder().decode(value.replace('-', '+').replace('_', '=').replace('~', '/'));
  }
}

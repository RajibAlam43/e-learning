package com.gii.api.service.live.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class GoogleMeetTokenCipherServiceTest {
  @Test
  void tokensAreRandomizedAuthenticatedAndBoundToTheirOwner() {
    var cipher = new GoogleMeetTokenCipherService();
    ReflectionTestUtils.setField(
        cipher, "encryptionKey", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
    String first = cipher.encrypt("refresh-secret", "teacher:1");
    String second = cipher.encrypt("refresh-secret", "teacher:1");
    assertThat(first).isNotEqualTo(second).doesNotContain("refresh-secret");
    assertThat(cipher.decrypt(first, "teacher:1")).isEqualTo("refresh-secret");
    assertThatThrownBy(() -> cipher.decrypt(first, "teacher:2"))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () -> cipher.decrypt(first.substring(0, first.length() - 4) + "AAAA", "teacher:1"))
        .isInstanceOf(IllegalStateException.class);
  }
}

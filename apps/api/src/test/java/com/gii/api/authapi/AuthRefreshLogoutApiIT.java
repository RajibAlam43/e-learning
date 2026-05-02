package com.gii.api.authapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.api.service.util.TokenHashUtil;
import com.gii.common.entity.user.User;
import com.gii.common.enums.UserStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class AuthRefreshLogoutApiIT extends AbstractAuthApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanAuthTables();
  }

  @Test
  void refreshWithValidCookieRotatesTokenAndReturnsAccessToken() throws Exception {
    User user = user("Refresh User", "refresh@example.com", null, "Secret123!", UserStatus.ACTIVE);
    user.setEmailVerifiedAt(Instant.now());
    userRepository.save(user);
    addRole(user, "STUDENT");

    String oldRawToken = "old-token-value";
    UUID sessionId = UUID.randomUUID();
    refreshToken(user, oldRawToken, sessionId, Instant.now().plusSeconds(3600));

    mockMvc
        .perform(
            post("/public/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie("refresh_token", oldRawToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(
            header().string("Set-Cookie", org.hamcrest.Matchers.containsString("refresh_token=")));

    var old =
        refreshTokenRepository.findAll().stream()
            .filter(token -> token.getTokenHash().equals(TokenHashUtil.hash(oldRawToken)))
            .findFirst()
            .orElseThrow();
    assertThat(old.getRevokedAt()).isNotNull();
    assertThat(old.getReplacedByToken()).isNotNull();
    assertThat(refreshTokenRepository.findBySessionIdAndRevokedAtIsNull(sessionId)).hasSize(1);
  }

  @Test
  void logoutWithTokenRevokesSessionAndClearsCookie() throws Exception {
    User user = user("Logout User", "logout@example.com", null, "Secret123!", UserStatus.ACTIVE);
    UUID sessionId = UUID.randomUUID();
    String rawToken = "session-token-1";
    refreshToken(user, rawToken, sessionId, Instant.now().plusSeconds(3600));
    refreshToken(user, "session-token-2", sessionId, Instant.now().plusSeconds(3600));

    mockMvc
        .perform(
            post("/public/auth/logout")
                .cookie(new jakarta.servlet.http.Cookie("refresh_token", rawToken)))
        .andExpect(status().isOk())
        .andExpect(
            header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));

    assertThat(refreshTokenRepository.findBySessionIdAndRevokedAtIsNull(sessionId)).isEmpty();
  }

  @Test
  void refreshTokenReuseIsRejectedAndSessionIsRevoked() throws Exception {
    User user = user("Reuse User", "reuse@example.com", null, "Secret123!", UserStatus.ACTIVE);
    user.setEmailVerifiedAt(Instant.now());
    userRepository.save(user);
    addRole(user, "STUDENT");

    String oldRawToken = "reused-token";
    UUID sessionId = UUID.randomUUID();
    refreshToken(user, oldRawToken, sessionId, Instant.now().plusSeconds(3600));

    mockMvc
        .perform(
            post("/public/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie("refresh_token", oldRawToken)))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/public/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie("refresh_token", oldRawToken)))
        .andExpect(status().isForbidden());

    assertThat(refreshTokenRepository.findBySessionIdAndRevokedAtIsNull(sessionId)).isEmpty();
  }
}

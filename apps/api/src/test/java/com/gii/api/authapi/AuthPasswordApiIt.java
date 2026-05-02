package com.gii.api.authapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.entity.user.User;
import com.gii.common.enums.UserStatus;
import com.gii.common.enums.VerificationChannel;
import com.gii.common.enums.VerificationPurpose;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class AuthPasswordApiIt extends AbstractAuthApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanAuthTables();
  }

  @Test
  void forgotPasswordUnknownUserReturnsOkWithoutCreatingCode() throws Exception {
    String body = "{\"channel\":\"EMAIL\",\"identifier\":\"missing@example.com\"}";

    mockMvc
        .perform(post("/public/auth/forgot-password").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isOk());

    assertThat(verificationCodeRepository.count()).isZero();
  }

  @Test
  void forgotPasswordExistingUserCreatesResetCode() throws Exception {
    User user = user("Forgot User", "forgot@example.com", null, "Secret123!", UserStatus.ACTIVE);
    String body = "{\"channel\":\"EMAIL\",\"identifier\":\"forgot@example.com\"}";

    mockMvc
        .perform(post("/public/auth/forgot-password").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isOk());

    assertThat(verificationCodeRepository.count()).isEqualTo(1);
  }

  @Test
  void resetPasswordUpdatesHashAndRevokesRefreshTokens() throws Exception {
    User user = user("Reset User", "reset@example.com", null, "OldSecret123!", UserStatus.ACTIVE);
    verificationCode(
        user,
        VerificationPurpose.PASSWORD_RESET,
        VerificationChannel.EMAIL,
        "reset@example.com",
        "123456",
        Instant.now().plusSeconds(1200));
    refreshToken(user, "reset-token-1", UUID.randomUUID(), Instant.now().plusSeconds(3600));

    String body =
        """
        {
          "channel":"EMAIL",
          "identifier":"reset@example.com",
          "code":"123456",
          "newPassword":"NewSecret123!"
        }
        """;

    mockMvc
        .perform(post("/public/auth/reset-password").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isOk());

    var updated = userRepository.findById(user.getId()).orElseThrow();
    assertThat(passwordEncoder.matches("NewSecret123!", updated.getPasswordHash())).isTrue();
    assertThat(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId())).isEmpty();
  }

  @Test
  void resetPasswordWithWeakPasswordReturns422() throws Exception {
    User user =
        user("Reset Weak", "reset-weak@example.com", null, "OldSecret123!", UserStatus.ACTIVE);
    verificationCode(
        user,
        VerificationPurpose.PASSWORD_RESET,
        VerificationChannel.EMAIL,
        "reset-weak@example.com",
        "654321",
        Instant.now().plusSeconds(1200));

    String body =
        """
        {
          "channel":"EMAIL",
          "identifier":"reset-weak@example.com",
          "code":"654321",
          "newPassword":"short"
        }
        """;

    mockMvc
        .perform(post("/public/auth/reset-password").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isUnprocessableEntity());
  }
}

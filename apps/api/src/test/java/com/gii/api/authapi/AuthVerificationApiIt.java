package com.gii.api.authapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.entity.user.User;
import com.gii.common.enums.UserStatus;
import com.gii.common.enums.VerificationChannel;
import com.gii.common.enums.VerificationPurpose;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class AuthVerificationApiIt extends AbstractAuthApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanAuthTables();
  }

  @Test
  void sendVerificationForExistingUnverifiedEmailCreatesCode() throws Exception {
    User user = user("Verify User", "verify@example.com", null, "Secret123!", UserStatus.ACTIVE);
    String body =
        """
        {
          "channel":"EMAIL",
          "identifier":"verify@example.com",
          "purpose":"EMAIL_VERIFICATION"
        }
        """;

    mockMvc
        .perform(post("/public/auth/send-code").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isOk());

    assertThat(verificationCodeRepository.count()).isEqualTo(1);
    verify(emailJobPublisherService, times(1)).publish(any());
  }

  @Test
  void sendVerificationUnknownIdentifierStillReturnsOk() throws Exception {
    String body =
        """
        {
          "channel":"EMAIL",
          "identifier":"unknown@example.com",
          "purpose":"EMAIL_VERIFICATION"
        }
        """;

    mockMvc
        .perform(post("/public/auth/send-code").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isOk());

    verify(emailJobPublisherService, never()).publish(any());
  }

  @Test
  void verifyCodeMarksEmailVerified() throws Exception {
    User user = user("Verify OTP", "otp@example.com", null, "Secret123!", UserStatus.ACTIVE);
    verificationCode(
        user,
        VerificationPurpose.EMAIL_VERIFICATION,
        VerificationChannel.EMAIL,
        "otp@example.com",
        "456789",
        Instant.now().plusSeconds(1200));

    String body =
        """
        {
          "channel":"EMAIL",
          "identifier":"otp@example.com",
          "code":"456789",
          "purpose":"EMAIL_VERIFICATION"
        }
        """;

    mockMvc
        .perform(post("/public/auth/verify-code").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isOk());

    assertThat(userRepository.findById(user.getId()).orElseThrow().getEmailVerifiedAt())
        .isNotNull();
  }
}

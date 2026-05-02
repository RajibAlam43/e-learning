package com.gii.api.authapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.entity.user.User;
import com.gii.common.enums.UserStatus;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class AuthLoginApiIt extends AbstractAuthApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanAuthTables();
  }

  @Test
  void loginVerifiedEmailReturnsAccessTokenAndRefreshCookie() throws Exception {
    User user =
        user("Verified User", "verified@example.com", null, "Secret123!", UserStatus.ACTIVE);
    user.setEmailVerifiedAt(Instant.now());
    userRepository.save(user);
    addRole(user, "STUDENT");

    String body =
        """
        {
          "channel":"EMAIL",
          "identifier":"verified@example.com",
          "password":"Secret123!"
        }
        """;

    mockMvc
        .perform(post("/public/auth/login").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isVerified").value(true))
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(
            header().string("Set-Cookie", org.hamcrest.Matchers.containsString("refresh_token=")))
        .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly")))
        .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Secure")))
        .andExpect(
            header().string("Set-Cookie", org.hamcrest.Matchers.containsString("SameSite=None")))
        .andExpect(
            header()
                .string(
                    "Set-Cookie",
                    org.hamcrest.Matchers.containsString("Path=/public/auth/refresh")));
  }

  @Test
  void loginUnverifiedEmailReturnsPendingVerificationAndCreatesOtp() throws Exception {
    User user =
        user("Unverified User", "pending@example.com", null, "Secret123!", UserStatus.ACTIVE);
    addRole(user, "STUDENT");

    String body =
        """
        {
          "channel":"EMAIL",
          "identifier":"pending@example.com",
          "password":"Secret123!"
        }
        """;

    mockMvc
        .perform(post("/public/auth/login").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isVerified").value(false))
        .andExpect(jsonPath("$.channel").value("EMAIL"))
        .andExpect(jsonPath("$.accessToken").value(nullValue()));

    assertThat(verificationCodeRepository.count()).isEqualTo(1);
  }
}

package com.gii.api.authapi;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Intent-first contract tests for expected status mappings that are currently missing. These tests
 * are expected to fail until exception/status mapping is aligned with API contract.
 */
class AuthApiContractGapIt extends AbstractAuthApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanAuthTables();
  }

  @Test
  void registerWithBothEmailAndPhoneShouldReturn400ByContract() throws Exception {
    String body =
        """
        {
          "fullName":"Gap Case",
          "email":"gap@example.com",
          "phoneNumber":"+8801712345678",
          "phoneCountryCode":"+880",
          "password":"Secret123!"
        }
        """;

    mockMvc
        .perform(post("/public/auth/register").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void loginWithInvalidCredentialsShouldReturn401ByContract() throws Exception {
    user("Wrong Pass", "wrongpass@example.com", null, "CorrectSecret123!", UserStatus.ACTIVE);

    String body =
        """
        {
          "channel":"EMAIL",
          "identifier":"wrongpass@example.com",
          "password":"incorrect"
        }
        """;

    mockMvc
        .perform(post("/public/auth/login").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void sendCodeRateLimitShouldReturn429ByContract() throws Exception {
    user("Rate User", "rate@example.com", null, "Secret123!", UserStatus.ACTIVE);

    String body =
        """
        {
          "channel":"EMAIL",
          "identifier":"rate@example.com",
          "purpose":"EMAIL_VERIFICATION"
        }
        """;

    mockMvc
        .perform(post("/public/auth/send-code").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isOk());
    mockMvc
        .perform(post("/public/auth/send-code").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isTooManyRequests());
  }
}

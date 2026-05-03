package com.gii.api.authapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class AuthRegisterApiIt extends AbstractAuthApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanAuthTables();
  }

  @Test
  void registerWithEmailCreatesUserRoleAndVerificationCode() throws Exception {
    String body =
        """
        {
          "fullName":"Rajib",
          "email":"rajib@example.com",
          "password":"Secret123!"
        }
        """;

    mockMvc
        .perform(post("/public/auth/register").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").exists())
        .andExpect(jsonPath("$.channel").value("EMAIL"));

    var user = userRepository.findByEmail("rajib@example.com");
    assertThat(user).isPresent();
    assertThat(userRoleRepository.findByUserId(user.get().getId())).hasSize(1);
    assertThat(verificationCodeRepository.count()).isEqualTo(1);
    verify(emailJobPublisherService, times(1)).publish(any());
  }

  @Test
  void registerWithPhoneCreatesUserAndPhoneVerificationCode() throws Exception {
    String body =
        """
        {
          "fullName":"Rajib",
          "phoneNumber":"+8801712345678",
          "phoneCountryCode":"+880",
          "password":"Secret123!"
        }
        """;

    mockMvc
        .perform(post("/public/auth/register").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.channel").value("PHONE"));

    verify(emailJobPublisherService, never()).publish(any());
  }

  @Test
  void registerWithWeakPasswordReturns422() throws Exception {
    String body =
        """
        {
          "fullName":"Rajib",
          "email":"weak@example.com",
          "password":"weak"
        }
        """;

    mockMvc
        .perform(post("/public/auth/register").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isUnprocessableEntity());
  }
}

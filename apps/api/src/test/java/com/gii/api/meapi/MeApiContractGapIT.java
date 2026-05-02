package com.gii.api.meapi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class MeApiContractGapIT extends AbstractMeApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupMeData();
  }

  @Test
  void invalidPhoneCountryCodeShouldBe400() throws Exception {
    var user = user("Gap User", "gap-user@example.com", "+8801710000012");
    profile(user, "bn-BD", null, null, null);

    mockMvc
        .perform(
            patch("/me/profile")
                .with(authentication(userAuth(user.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phoneCountryCode\":\"abc\"}"))
        .andExpect(status().isBadRequest());
  }
}

package com.gii.api.meapi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class MeUpdateApiIt extends AbstractMeApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupMeData();
  }

  @Test
  void updateProfileNormalizesAndPersistsUserAndProfileFields() throws Exception {
    var user = user("Old Name", "old@example.com", "+8801710000002");
    profile(user, "bn-BD", null, null, null);
    var other = user("Other", "exists@example.com", "+8801719999999");

    String updateBody =
        """
        {
          "fullName":"  New Name  ",
          "email":"NEW@EXAMPLE.COM",
          "phone":"+8801710000011",
          "phoneCountryCode":"880",
          "avatarUrl":" https://cdn.test/new.png ",
          "bio":" About me ",
          "locale":" en-US ",
          "timezone":"Asia/Dhaka",
          "displayName":"Ustadh New",
          "headline":"Lead Teacher",
          "specialties":["aqeedah","fiqh"],
          "yearsExperience":12
        }
        """;

    mockMvc
        .perform(
            patch("/me/profile")
                .with(authentication(userAuth(user.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fullName").value("New Name"))
        .andExpect(jsonPath("$.email").value("new@example.com"))
        .andExpect(jsonPath("$.phone").value("+8801710000011"))
        .andExpect(jsonPath("$.phoneCountryCode").value("+880"))
        .andExpect(jsonPath("$.avatarUrl").value("https://cdn.test/new.png"))
        .andExpect(jsonPath("$.bio").value("About me"))
        .andExpect(jsonPath("$.locale").value("en-US"))
        .andExpect(jsonPath("$.instructorProfile.displayName").value("Ustadh New"))
        .andExpect(jsonPath("$.instructorProfile.yearsExperience").value(12));
  }

  @Test
  void updateProfileReturns409ForEmailOrPhoneConflict() throws Exception {
    var user = user("User A", "ua@example.com", "+8801710000003");
    var other = user("User B", "ub@example.com", "+8801710000004");
    profile(user, "bn-BD", null, null, null);

    mockMvc
        .perform(
            patch("/me/profile")
                .with(authentication(userAuth(user.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ub@example.com\"}"))
        .andExpect(status().isConflict());

    mockMvc
        .perform(
            patch("/me/profile")
                .with(authentication(userAuth(user.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"+8801710000004\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  void updateProfileRejectsTooLongPhoneCountryCode() throws Exception {
    var user = user("User Len", "ulen@example.com", "+8801710000005");
    profile(user, "bn-BD", null, null, null);

    mockMvc
        .perform(
            patch("/me/profile")
                .with(authentication(userAuth(user.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phoneCountryCode\":\"123456\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateProfileAllowsLongFullNameBecauseSchemaUsesText() throws Exception {
    var user = user("Short", "longname@example.com", "+8801710000006");
    profile(user, "bn-BD", null, null, null);
    String longName = "A".repeat(600);

    mockMvc
        .perform(
            patch("/me/profile")
                .with(authentication(userAuth(user.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"" + longName + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fullName").value(longName));
  }
}

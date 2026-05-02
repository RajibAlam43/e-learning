package com.gii.api.meapi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class MeGetApiIt extends AbstractMeApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupMeData();
  }

  @Test
  void getMeReturnsProfileRolesAndAggregates() throws Exception {
    var user = user("Learner One", "learner-one@example.com", "+8801710000001");
    profile(user, "en-US", "Asia/Dhaka", "https://cdn.test/a.png", "bio");
    instructorProfile(user, "Teacher One");
    var creator = user("Creator", "creator-me1@example.com", "+8801710000099");
    var course1 = course("Course 1", "course-1", creator);
    var course2 = course("Course 2", "course-2", creator);
    enrollment(user, course1, true);
    enrollment(user, course2, false);
    certificate(user, course1, "GII-CERT-ME111111", false, creator);
    var sec = section(course1, 1);
    var les = lesson(course1, sec, 1);
    var lc = liveClass(course1, sec, les, creator);
    attendance(user, lc);

    mockMvc
        .perform(get("/me").with(authentication(userAuth(user.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fullName").value("Learner One"))
        .andExpect(jsonPath("$.locale").value("en-US"))
        .andExpect(jsonPath("$.instructorProfile.displayName").value("Teacher One"))
        .andExpect(jsonPath("$.totalEnrolledCourses").value(2))
        .andExpect(jsonPath("$.completedCourses").value(1))
        .andExpect(jsonPath("$.earnedCertificates").value(1))
        .andExpect(jsonPath("$.totalLiveClassesAttended").value(1));
  }
}

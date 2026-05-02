package com.gii.api.adminapi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class AdminApiContractGapIt extends AbstractAdminApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupAdminData();
  }

  @Test
  void updateMediaAssetShouldBeAvailableAtSingleAdminPrefixPath() throws Exception {
    var admin = user("Admin Gap", "admin-gap@example.com");
    var creator = user("Creator Gap", "creator-gap@example.com");
    var course = course("Gap Course", "gap-course", creator);
    var sec = section(course, 1);
    var lesson = lesson(course, sec, 1);
    var asset = mediaAsset(lesson, "gap-playback");

    mockMvc
        .perform(
            patch("/admin/media-assets/{mediaAssetId}", asset.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Should Work\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void publishCourseWithoutStructureShouldBeRejectedByContract() throws Exception {
    var admin = user("Admin Publish Gap", "admin-publish-gap@example.com");
    var creator = user("Creator Publish Gap", "creator-publish-gap@example.com");
    var course = course("Publish Gap", "publish-gap", creator);

    mockMvc
        .perform(
            post("/admin/courses/{courseId}/publish", course.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isBadRequest());
  }
}

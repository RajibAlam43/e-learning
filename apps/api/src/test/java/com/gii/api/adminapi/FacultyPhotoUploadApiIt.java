package com.gii.api.adminapi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestPropertySource(
    properties = {
      "assets.base-url=https://assets.test",
      "storage.r2.account-id=test-account",
      "storage.r2.access-key-id=test-access",
      "storage.r2.secret-access-key=test-secret",
      "storage.r2.bucket=test-bucket",
      "storage.r2.public-bucket=test-public-bucket"
    })
class FacultyPhotoUploadApiIt extends AbstractAdminApiIntegrationTest {
  @Autowired MockMvc mvc;

  @Test
  void facultyPhotosUseThePublicBucketAndSeparateFacultyKeys() throws Exception {
    final var admin = user("Faculty photo admin", "faculty-photo@example.com");
    mvc.perform(
            post("/admin/thumbnails/upload-url")
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"ownerType":"FACULTY","filename":"portrait.png",
                     "contentType":"image/png","sizeBytes":1024}
                    """))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.objectKey")
                .value(
                    org.hamcrest.Matchers.matchesPattern(
                        "thumbnails/faculty/portrait-[0-9a-f]{8}\\.png")))
        .andExpect(
            jsonPath("$.uploadUrl")
                .value(
                    org.hamcrest.Matchers.startsWith(
                        "https://test-account.r2.cloudflarestorage.com/test-public-bucket/thumbnails/faculty/")))
        .andExpect(jsonPath("$.method").value("PUT"));
  }
}

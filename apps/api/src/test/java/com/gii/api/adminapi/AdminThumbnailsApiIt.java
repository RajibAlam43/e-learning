package com.gii.api.adminapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.PublishStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@TestPropertySource(
    properties = {
      "assets.base-url=https://assets.test",
      "storage.r2.account-id=test-account",
      "storage.r2.access-key-id=test-access",
      "storage.r2.secret-access-key=test-secret",
      "storage.r2.bucket=test-bucket"
    })
class AdminThumbnailsApiIt extends AbstractAdminApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupAdminData();
  }

  @Test
  void adminShouldCreateDirectThumbnailUploadUrl() throws Exception {
    var admin = user("Upload Admin", "upload-thumbnail-admin@example.com");

    mockMvc
        .perform(
            post("/admin/thumbnails/upload-url")
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "ownerType":"COURSE",
                      "filename":"course-cover.webp",
                      "contentType":"image/webp",
                      "sizeBytes":1024
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.objectKey").value(org.hamcrest.Matchers.matchesPattern(
            "thumbnails/courses/course-cover-[0-9a-f]{8}\\.webp")))
        .andExpect(jsonPath("$.uploadUrl").isNotEmpty())
        .andExpect(jsonPath("$.method").value("PUT"))
        .andExpect(jsonPath("$.contentType").value("image/webp"))
        .andExpect(jsonPath("$.sizeBytes").value(1024))
        .andExpect(jsonPath("$.expiresAt").isNotEmpty());
  }

  @Test
  void thumbnailUploadShouldRejectFilesLargerThanTwentyMegabytes() throws Exception {
    var admin = user("Large Upload Admin", "large-upload-thumbnail-admin@example.com");

    mockMvc
        .perform(
            post("/admin/thumbnails/upload-url")
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "ownerType":"COURSE",
                      "filename":"oversized.webp",
                      "contentType":"image/webp",
                      "sizeBytes":20971521
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void existingAdminEndpointsShouldPersistAndResolveStandardThumbnailKeys() throws Exception {
    var admin = user("Thumbnail Admin", "thumbnail-admin@example.com");
    var creator = user("Thumbnail Creator", "thumbnail-creator@example.com");
    var course = course("Thumbnail Course", "thumbnail-course", creator);
    var collection =
        collection("Thumbnail Pack", "thumbnail-pack", creator, PublishStatus.DRAFT);
    var section = section(course, 1);
    var lesson = lesson(course, section, 1);
    var mediaAsset = mediaAsset(lesson, "thumbnail-playback");

    String courseKey = "thumbnails/courses/course-a1b2c3d4.webp";
    String collectionKey = "thumbnails/collections/collection-b2c3d4e5.webp";
    String mediaAssetKey = "thumbnails/media-assets/media-c3d4e5f6.webp";

    patchThumbnail("/admin/courses/{id}", course.getId(), courseKey, admin.getId())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.thumbnailObjectKey").value(courseKey))
        .andExpect(jsonPath("$.thumbnailUrl").value("https://assets.test/" + courseKey));

    patchThumbnail(
            "/admin/collections/{id}", collection.getId(), collectionKey, admin.getId())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.thumbnailObjectKey").value(collectionKey))
        .andExpect(jsonPath("$.thumbnailUrl").value("https://assets.test/" + collectionKey));

    patchThumbnail(
            "/admin/media-assets/{id}", mediaAsset.getId(), mediaAssetKey, admin.getId())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.thumbnailObjectKey").value(mediaAssetKey))
        .andExpect(jsonPath("$.thumbnailUrl").value("https://assets.test/" + mediaAssetKey));

    assertThat(courseRepository.findById(course.getId()).orElseThrow().getThumbnailObjectKey())
        .isEqualTo(courseKey);
    assertThat(
            collectionRepository
                .findById(collection.getId())
                .orElseThrow()
                .getThumbnailObjectKey())
        .isEqualTo(collectionKey);
    assertThat(
            mediaAssetRepository
                .findById(mediaAsset.getId())
                .orElseThrow()
                .getThumbnailObjectKey())
        .isEqualTo(mediaAssetKey);
  }

  @Test
  void blankShouldClearThumbnailAndWrongOwnerKeyShouldBeRejected() throws Exception {
    var admin = user("Clear Admin", "clear-thumbnail-admin@example.com");
    var creator = user("Clear Creator", "clear-thumbnail-creator@example.com");
    var course = course("Clear Thumbnail Course", "clear-thumbnail-course", creator);
    course.setThumbnailObjectKey(
        "thumbnails/courses/original-a1b2c3d4.webp");
    courseRepository.saveAndFlush(course);

    patchThumbnail("/admin/courses/{id}", course.getId(), "", admin.getId())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.thumbnailObjectKey").doesNotExist())
        .andExpect(jsonPath("$.thumbnailUrl").doesNotExist());

    assertThat(courseRepository.findById(course.getId()).orElseThrow().getThumbnailObjectKey())
        .isNull();

    String wrongOwnerKey = "courses/" + java.util.UUID.randomUUID() + "/thumbnails/image.webp";
    patchThumbnail(
            "/admin/courses/{id}", course.getId(), wrongOwnerKey, admin.getId())
        .andExpect(status().isBadRequest());
  }

  private org.springframework.test.web.servlet.ResultActions patchThumbnail(
      String path, java.util.UUID entityId, String objectKey, java.util.UUID adminId)
      throws Exception {
    return mockMvc.perform(
        patch(path, entityId)
            .with(authentication(adminAuth(adminId)))
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"thumbnailObjectKey":"%s"}
                """
                    .formatted(objectKey)));
  }
}

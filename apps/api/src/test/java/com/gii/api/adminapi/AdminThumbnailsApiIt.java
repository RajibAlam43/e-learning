package com.gii.api.adminapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.PublishStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@TestPropertySource(properties = "assets.base-url=https://assets.test")
class AdminThumbnailsApiIt extends AbstractAdminApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupAdminData();
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

    String courseKey = "courses/" + course.getId() + "/thumbnails/course.webp";
    String collectionKey =
        "collections/" + collection.getId() + "/thumbnails/collection.webp";
    String lessonKey = "lessons/" + lesson.getId() + "/thumbnails/lesson.webp";

    patchThumbnail("/admin/courses/{id}", course.getId(), courseKey, admin.getId())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.thumbnailObjectKey").value(courseKey))
        .andExpect(jsonPath("$.thumbnailUrl").value("https://assets.test/" + courseKey));

    patchThumbnail(
            "/admin/collections/{id}", collection.getId(), collectionKey, admin.getId())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.thumbnailObjectKey").value(collectionKey))
        .andExpect(jsonPath("$.thumbnailUrl").value("https://assets.test/" + collectionKey));

    patchThumbnail("/admin/lessons/{id}", lesson.getId(), lessonKey, admin.getId())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.thumbnailObjectKey").value(lessonKey))
        .andExpect(jsonPath("$.thumbnailUrl").value("https://assets.test/" + lessonKey));

    assertThat(courseRepository.findById(course.getId()).orElseThrow().getThumbnailObjectKey())
        .isEqualTo(courseKey);
    assertThat(
            collectionRepository
                .findById(collection.getId())
                .orElseThrow()
                .getThumbnailObjectKey())
        .isEqualTo(collectionKey);
    assertThat(lessonRepository.findById(lesson.getId()).orElseThrow().getThumbnailObjectKey())
        .isEqualTo(lessonKey);
  }

  @Test
  void blankShouldClearThumbnailAndWrongOwnerKeyShouldBeRejected() throws Exception {
    var admin = user("Clear Admin", "clear-thumbnail-admin@example.com");
    var creator = user("Clear Creator", "clear-thumbnail-creator@example.com");
    var course = course("Clear Thumbnail Course", "clear-thumbnail-course", creator);
    course.setThumbnailObjectKey(
        "courses/" + course.getId() + "/thumbnails/original.webp");
    courseRepository.saveAndFlush(course);

    patchThumbnail("/admin/courses/{id}", course.getId(), "", admin.getId())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.thumbnailObjectKey").doesNotExist())
        .andExpect(jsonPath("$.thumbnailUrl").doesNotExist());

    assertThat(courseRepository.findById(course.getId()).orElseThrow().getThumbnailObjectKey())
        .isNull();

    String wrongOwnerKey =
        "courses/" + java.util.UUID.randomUUID() + "/thumbnails/image.webp";
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

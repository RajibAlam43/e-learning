package com.gii.api.adminapi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.PublishStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class AdminCollectionsApiIt extends AbstractAdminApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupAdminData();
  }

  @Test
  void adminCanCreateSetCoursesPublishAndListCollections() throws Exception {
    var admin = user("Admin", "admin-collections@example.com");
    var creator = user("Creator", "creator-collections@example.com");
    var courseA = course("Course A", "course-a-admin-col", creator, PublishStatus.PUBLISHED);
    var courseB = course("Course B", "course-b-admin-col", creator, PublishStatus.PUBLISHED);

    String createdBody =
        mockMvc
            .perform(
                post("/admin/collections")
                    .with(authentication(adminAuth(admin.getId())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "title":"Analytics Pack",
                          "slug":"analytics-pack-admin",
                          "collectionType":"PACK",
                          "priceBdt":4500
                        }
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Analytics Pack"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String collectionId = com.jayway.jsonpath.JsonPath.read(createdBody, "$.collectionId");

    mockMvc
        .perform(
            post("/admin/collections/{collectionId}/courses", collectionId)
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "items":[
                        {"courseId":"%s","position":1,"isMandatory":true},
                        {"courseId":"%s","position":2,"isMandatory":false}
                      ]
                    }
                    """
                        .formatted(courseA.getId(), courseB.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.courses.length()").value(2));

    mockMvc
        .perform(
            post("/admin/collections/{collectionId}/publish", collectionId)
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/admin/collections").with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].title").value("Analytics Pack"))
        .andExpect(jsonPath("$[0].status").value("PUBLISHED"))
        .andExpect(jsonPath("$[0].courseCount").value(2));

    mockMvc
        .perform(
            patch("/admin/collections/{collectionId}", collectionId)
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"shortDescription\":\"Updated short description\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.shortDescription").value("Updated short description"));
  }
}

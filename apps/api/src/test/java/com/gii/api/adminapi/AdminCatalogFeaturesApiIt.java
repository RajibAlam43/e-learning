package com.gii.api.adminapi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.LessonType;
import com.gii.common.enums.PublishStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

class AdminCatalogFeaturesApiIt extends AbstractAdminApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupAdminData();
  }

  @Test
  void pdfLessonRequiresOnePrimaryPdfAndProtectsItWhilePublished() throws Exception {
    var admin = user("PDF Admin", "pdf-admin@example.com");
    var creator = user("PDF Creator", "pdf-creator@example.com");
    var course = course("PDF Course", "pdf-course", creator);
    var lesson = lesson(course, section(course, 1), 1);
    lesson.setLessonType(LessonType.PDF);
    lessonRepository.save(lesson);

    mockMvc
        .perform(
            post("/admin/lessons/{lessonId}/publish", lesson.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isBadRequest());

    createResource(admin.getId(), lesson.getId(), "Notes", "SUPPLEMENTARY", 1, "notes-abcdef12.pdf")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.purpose").value("SUPPLEMENTARY"));

    String primaryResponse =
        createResource(
                admin.getId(),
                lesson.getId(),
                "Main PDF",
                "PRIMARY_CONTENT",
                2,
                "main-abcdef12.pdf")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.purpose").value("PRIMARY_CONTENT"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String primaryId =
        com.fasterxml.jackson.databind.json.JsonMapper.builder()
            .build()
            .readTree(primaryResponse)
            .get("resourceId")
            .asText();

    createResource(
            admin.getId(), lesson.getId(), "Other Main", "PRIMARY_CONTENT", 3, "other-abcdef12.pdf")
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/admin/lessons/{lessonId}/publish", lesson.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            patch("/admin/lesson-resources/{resourceId}", primaryId)
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"purpose\":\"SUPPLEMENTARY\"}"))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            delete("/admin/lesson-resources/{resourceId}", primaryId)
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void featuredCoursesArePublishedOrderedAndRemovedOnUnpublish() throws Exception {
    var admin = user("Feature Admin", "feature-admin@example.com");
    var creator = user("Feature Creator", "feature-creator@example.com");
    var first = course("First", "featured-first", creator, PublishStatus.PUBLISHED);
    var second = course("Second", "featured-second", creator, PublishStatus.PUBLISHED);
    var draft = course("Draft", "featured-draft", creator);

    feature(admin.getId(), first.getId().toString(), 2).andExpect(status().isOk());
    feature(admin.getId(), second.getId().toString(), 1).andExpect(status().isOk());
    feature(admin.getId(), draft.getId().toString(), 3).andExpect(status().isBadRequest());

    mockMvc
        .perform(get("/public/courses/featured"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].title").value("Second"))
        .andExpect(jsonPath("$[1].title").value("First"));

    mockMvc
        .perform(
            post("/admin/courses/{courseId}/unpublish", second.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/public/courses/featured"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].title").value("First"));
  }

  @Test
  void publicSettingsExposeOnlyExplicitlyPublicNonSensitiveValues() throws Exception {
    var admin = user("Settings Admin", "settings-admin@example.com");

    upsertSetting(admin.getId(), "homepage.hero", true, "Welcome").andExpect(status().isOk());
    upsertSetting(admin.getId(), "internal.flags", false, "Hidden").andExpect(status().isOk());
    upsertSetting(admin.getId(), "payment.api_key", true, "Never expose")
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(get("/public/settings"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].key").value("homepage.hero"))
        .andExpect(jsonPath("$[0].value.title").value("Welcome"))
        .andExpect(jsonPath("$[0].description").doesNotExist())
        .andExpect(jsonPath("$[0].isPublic").doesNotExist());

    upsertSetting(admin.getId(), "student.forbidden", true, "No").andExpect(status().isOk());
    mockMvc
        .perform(
            put("/admin/settings/student.forbidden")
                .with(authentication(studentAuthentication()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\":{},\"isPublic\":true}"))
        .andExpect(status().isForbidden());
  }

  private org.springframework.test.web.servlet.ResultActions createResource(
      java.util.UUID adminId,
      java.util.UUID lessonId,
      String title,
      String purpose,
      int position,
      String filename)
      throws Exception {
    return mockMvc.perform(
        post("/admin/lessons/{lessonId}/resources", lessonId)
            .with(authentication(adminAuth(adminId)))
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                  "title":"%s",
                  "resourceType":"PDF",
                  "purpose":"%s",
                  "mimeType":"application/pdf",
                  "objectKey":"lesson-resources/%s/%s",
                  "position":%d
                }
                """
                    .formatted(title, purpose, lessonId, filename, position)));
  }

  private org.springframework.test.web.servlet.ResultActions feature(
      java.util.UUID adminId, String courseId, int position) throws Exception {
    return mockMvc.perform(
        post("/admin/courses/{courseId}/feature", courseId)
            .with(authentication(adminAuth(adminId)))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"position\":" + position + "}"));
  }

  private org.springframework.test.web.servlet.ResultActions upsertSetting(
      java.util.UUID adminId, String key, boolean isPublic, String title) throws Exception {
    return mockMvc.perform(
        put("/admin/settings/{key}", key)
            .with(authentication(adminAuth(adminId)))
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"value\":{\"title\":\""
                    + title
                    + "\"},\"description\":\"Admin only metadata\",\"isPublic\":"
                    + isPublic
                    + "}"));
  }

  private UsernamePasswordAuthenticationToken studentAuthentication() {
    return new UsernamePasswordAuthenticationToken(
        java.util.UUID.randomUUID(),
        null,
        java.util.List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
  }
}

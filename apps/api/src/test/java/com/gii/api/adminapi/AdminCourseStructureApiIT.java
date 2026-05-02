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

class AdminCourseStructureApiIt extends AbstractAdminApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupAdminData();
  }

  @Test
  void createGetUpdateAndPublishCourseShouldPersistState() throws Exception {
    var admin = user("Admin One", "admin-course@example.com");

    mockMvc
        .perform(
            post("/admin/courses")
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title":"Course Alpha",
                      "slug":"course-alpha",
                      "priceBdt":1500,
                      "level":"BEGINNER",
                      "language":"EN",
                      "studyMode":"SCHEDULED",
                      "isFree":false
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Course Alpha"))
        .andExpect(jsonPath("$.status").value("DRAFT"));

    var course =
        courseRepository.findAll().stream()
            .filter(c -> "course-alpha".equals(c.getSlug()))
            .findFirst()
            .orElseThrow();
    mockMvc
        .perform(
            get("/admin/courses/{courseId}", course.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.courseId").value(course.getId().toString()));

    mockMvc
        .perform(
            patch("/admin/courses/{courseId}", course.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Course Alpha Updated\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Course Alpha Updated"));

    mockMvc
        .perform(
            post("/admin/courses/{courseId}/sections", course.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"title\":\"Section Publish\",\"slug\":\"section-publish\",\"position\":1}"))
        .andExpect(status().isOk());

    var section =
        courseSectionRepository.findByCourseIdOrderByPositionAsc(course.getId()).getFirst();
    mockMvc
        .perform(
            post("/admin/sections/{sectionId}/lessons", section.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"Lesson Publish","slug":"lesson-publish","position":1,"lessonType":"VIDEO"}
                    """))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/admin/courses/{courseId}/publish", course.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk());

    var updated = courseRepository.findById(course.getId()).orElseThrow();
    org.assertj.core.api.Assertions.assertThat(updated.getStatus())
        .isEqualTo(PublishStatus.PUBLISHED);
    org.assertj.core.api.Assertions.assertThat(updated.getPublishedAt()).isNotNull();
  }

  @Test
  void sectionAndLessonLifecycleShouldPersist() throws Exception {
    var admin = user("Admin Two", "admin-structure@example.com");
    var creator = user("Creator Two", "creator-structure@example.com");
    var course = course("Course Struct", "course-struct", creator);

    mockMvc
        .perform(
            post("/admin/courses/{courseId}/sections", course.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Section A\",\"slug\":\"section-a\",\"position\":1}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Section A"));

    var section =
        courseSectionRepository.findByCourseIdOrderByPositionAsc(course.getId()).getFirst();

    mockMvc
        .perform(
            post("/admin/sections/{sectionId}/lessons", section.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"Lesson A","slug":"lesson-a","position":1,"lessonType":"VIDEO"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Lesson A"));

    var lesson = lessonRepository.findByCourseIdOrderByPositionAsc(course.getId()).getFirst();
    mockMvc
        .perform(
            post("/admin/courses/{courseId}/structure/reorder", course.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "sections":[
                        {
                          "sectionId":"%s",
                          "newPosition":2,
                          "lessons":[{"lessonId":"%s","newPosition":3}]
                        }
                      ]
                    }
                    """
                        .formatted(section.getId(), lesson.getId())))
        .andExpect(status().isOk());

    org.assertj.core.api.Assertions.assertThat(
            courseSectionRepository.findById(section.getId()).orElseThrow().getPosition())
        .isEqualTo(2);
    org.assertj.core.api.Assertions.assertThat(
            lessonRepository.findById(lesson.getId()).orElseThrow().getPosition())
        .isEqualTo(3);
  }
}

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
                          "items":[{"itemId":"%s","itemType":"LESSON","newPosition":3}]
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

  @Test
  void sectionItemsShouldReturnMixedOrderedLessonsAndQuizzes() throws Exception {
    var admin = user("Admin Three", "admin-items@example.com");
    var creator = user("Creator Three", "creator-items@example.com");
    var course = course("Course Items", "course-items", creator);
    var section = section(course, 1);
    var lesson = lesson(course, section, 1);

    mockMvc
        .perform(
            post("/admin/sections/{sectionId}/quizzes", section.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "sectionId":"%s",
                      "position":2,
                      "title":"Quiz Mixed",
                      "passingScorePct":60,
                      "maxAttempts":3,
                      "timeLimitSec":600,
                      "questions":[
                        {"position":1,"questionText":"Q1","questionType":"MCQ","points":1,"choices":[
                          {"choiceText":"A","isCorrect":true}
                        ]}
                      ]
                    }
                    """
                        .formatted(section.getId())))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/admin/courses/{courseId}", course.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sections[0].items[0].itemType").value("LESSON"))
        .andExpect(jsonPath("$.sections[0].items[0].position").value(1))
        .andExpect(jsonPath("$.sections[0].items[1].itemType").value("QUIZ"))
        .andExpect(jsonPath("$.sections[0].items[1].position").value(2))
        .andExpect(jsonPath("$.sections[0].items[0].lesson.title").value(lesson.getTitle()))
        .andExpect(jsonPath("$.sections[0].items[1].quiz.title").value("Quiz Mixed"));
  }

  @Test
  void createQuizShouldRejectSectionMismatch() throws Exception {
    var admin = user("Admin Four", "admin-mismatch@example.com");
    var creator = user("Creator Four", "creator-mismatch@example.com");
    var course = course("Course Mismatch", "course-mismatch", creator);
    var sectionA = section(course, 1);
    var sectionB = section(course, 2);

    mockMvc
        .perform(
            post("/admin/sections/{sectionId}/quizzes", sectionA.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "sectionId":"%s",
                      "position":1,
                      "title":"Quiz Wrong Section",
                      "passingScorePct":60,
                      "maxAttempts":3,
                      "questions":[
                        {"position":1,"questionText":"Q1","questionType":"MCQ","points":1,"choices":[
                          {"choiceText":"A","isCorrect":true}
                        ]}
                      ]
                    }
                    """
                        .formatted(sectionB.getId())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void reorderStructureShouldValidateDuplicateItemPositions() throws Exception {
    var admin = user("Admin Five", "admin-reorder-validation@example.com");
    var creator = user("Creator Five", "creator-reorder-validation@example.com");
    var course = course("Course Reorder", "course-reorder", creator);
    var section = section(course, 1);
    var lesson = lesson(course, section, 1);
    var quiz = quiz(course, "Quiz Reorder");

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
                          "newPosition":1,
                          "items":[
                            {"itemId":"%s","itemType":"LESSON","newPosition":1},
                            {"itemId":"%s","itemType":"QUIZ","newPosition":1}
                          ]
                        }
                      ]
                    }
                    """
                        .formatted(section.getId(), lesson.getId(), quiz.getId())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void reorderStructureShouldUpdateMixedItemPositions() throws Exception {
    var admin = user("Admin Six", "admin-reorder-mixed@example.com");
    var creator = user("Creator Six", "creator-reorder-mixed@example.com");
    var course = course("Course Reorder Mixed", "course-reorder-mixed", creator);
    var section = section(course, 1);
    var lesson = lesson(course, section, 1);
    var quiz = quiz(course, "Quiz Mixed Reorder");

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
                          "newPosition":1,
                          "items":[
                            {"itemId":"%s","itemType":"LESSON","newPosition":2},
                            {"itemId":"%s","itemType":"QUIZ","newPosition":1}
                          ]
                        }
                      ]
                    }
                    """
                        .formatted(section.getId(), lesson.getId(), quiz.getId())))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/admin/courses/{courseId}", course.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sections[0].items[0].itemType").value("QUIZ"))
        .andExpect(jsonPath("$.sections[0].items[0].position").value(1))
        .andExpect(jsonPath("$.sections[0].items[1].itemType").value("LESSON"))
        .andExpect(jsonPath("$.sections[0].items[1].position").value(2));
  }
}

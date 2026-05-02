package com.gii.api.lessonapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.entity.enrollment.LessonProgressId;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.ReleaseType;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class LessonProgressApiIT extends AbstractLessonApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupLessonData();
  }

  @Test
  void saveProgressCreatesOrUpdatesProgressRow() throws Exception {
    var creator = user("Creator", "creator-progress@example.com");
    var student = user("Student", "student-progress@example.com");
    var course = course("Progress Course", "progress-course", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson =
        lesson(course, sec, 1, PublishStatus.PUBLISHED, false, ReleaseType.IMMEDIATE, null, null);
    enrollment(student, course, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(3600));

    mockMvc
        .perform(
            post("/learn/lessons/{lessonId}/progress", lesson.getId())
                .with(authentication(studentAuth(student.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"completed\":false,\"lastPositionSec\":95}"))
        .andExpect(status().isOk());

    var saved =
        lessonProgressRepository
            .findById(
                LessonProgressId.builder().userId(student.getId()).lessonId(lesson.getId()).build())
            .orElseThrow();
    assertThat(saved.getLastPositionSec()).isEqualTo(95);
    assertThat(saved.getCompletedAt()).isNull();
  }

  @Test
  void markCompleteSetsCompletedAt() throws Exception {
    var creator = user("Creator", "creator-complete@example.com");
    var student = user("Student", "student-complete@example.com");
    var course = course("Complete Course", "complete-course", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson =
        lesson(course, sec, 1, PublishStatus.PUBLISHED, false, ReleaseType.IMMEDIATE, null, null);
    enrollment(student, course, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(3600));

    mockMvc
        .perform(
            post("/learn/lessons/{lessonId}/complete", lesson.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk());

    var saved =
        lessonProgressRepository
            .findById(
                LessonProgressId.builder().userId(student.getId()).lessonId(lesson.getId()).build())
            .orElseThrow();
    assertThat(saved.getCompletedAt()).isNotNull();
  }
}

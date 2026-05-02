package com.gii.api.lessonapi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.MediaProvider;
import com.gii.common.enums.MediaStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.ReleaseType;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class LessonApiContractGapIt extends AbstractLessonApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupLessonData();
  }

  @Test
  void saveProgressNegativePositionShouldBe400() throws Exception {
    var creator = user("Creator", "creator-gap1@example.com");
    var student = user("Student", "student-gap1@example.com");
    var course = course("Gap Course 1", "gap-course-1", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson =
        lesson(course, sec, 1, PublishStatus.PUBLISHED, false, ReleaseType.IMMEDIATE, null, null);
    enrollment(student, course, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(3600));

    mockMvc
        .perform(
            post("/learn/lessons/{lessonId}/progress", lesson.getId())
                .with(authentication(studentAuth(student.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"completed\":false,\"lastPositionSec\":-1}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void playbackUnknownLessonShouldBe404() throws Exception {
    var student = user("Student", "student-gap2@example.com");

    mockMvc
        .perform(
            get("/learn/lessons/{lessonId}/playback", java.util.UUID.randomUUID())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isNotFound());
  }

  @Test
  void playbackMediaNotReadyShouldBe404() throws Exception {
    var creator = user("Creator", "creator-gap3@example.com");
    var student = user("Student", "student-gap3@example.com");
    var course = course("Gap Course 3", "gap-course-3", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson =
        lesson(course, sec, 1, PublishStatus.PUBLISHED, false, ReleaseType.IMMEDIATE, null, null);
    enrollment(student, course, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(3600));
    mediaAsset(lesson, MediaProvider.BUNNY, MediaStatus.PROCESSING);

    mockMvc
        .perform(
            get("/learn/lessons/{lessonId}/playback", lesson.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isNotFound());
  }
}

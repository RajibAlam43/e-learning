package com.gii.api.instructorapi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.InstructorRole;
import com.gii.common.enums.PublishStatus;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class InstructorApiContractGapIt extends AbstractInstructorApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupInstructorData();
  }

  @Test
  void createLiveClassWithBlankTitleShouldBe400() throws Exception {
    var creator = user("Creator", "creator-inst-gap@example.com");
    var instructor = user("Instructor", "inst-gap@example.com");
    var course = course("Gap Course", "gap-course-inst", creator, PublishStatus.PUBLISHED);
    assignment(course, instructor, InstructorRole.PRIMARY);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson = lesson(course, sec, 1, PublishStatus.PUBLISHED);

    String invalidBody =
        """
        {
          "sectionId":"%s",
          "lessonId":"%s",
          "title":"   ",
          "startsAt":"%s",
          "endsAt":"%s"
        }
        """
            .formatted(
                sec.getId(),
                lesson.getId(),
                Instant.now().plusSeconds(1800).toString(),
                Instant.now().plusSeconds(3600).toString());

    mockMvc
        .perform(
            post("/instructor/courses/{courseId}/live-classes", course.getId())
                .with(authentication(instructorAuth(instructor.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidBody))
        .andExpect(status().isBadRequest());
  }
}

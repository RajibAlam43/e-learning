package com.gii.api.instructorapi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.InstructorRole;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.enums.PublishStatus;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class InstructorLiveClassesApiIt extends AbstractInstructorApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupInstructorData();
  }

  @Test
  void createStartUpdateDeleteFlowWorksForAssignedInstructor() throws Exception {
    var creator = user("Creator", "creator-inst-live@example.com");
    var instructor = user("Instructor", "inst-live@example.com");
    var course = course("Course Live", "course-live-inst", creator, PublishStatus.PUBLISHED);
    assignment(course, instructor, InstructorRole.PRIMARY);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson = lesson(course, sec, 1, PublishStatus.PUBLISHED);
    Instant startsAt = Instant.now().plusSeconds(3600);
    Instant endsAt = Instant.now().plusSeconds(5400);
    String createBody =
        """
        {
          "sectionId":"%s",
          "lessonId":"%s",
          "title":" Weekly Session ",
          "description":"Live review",
          "startsAt":"%s",
          "endsAt":"%s",
          "provider":"ZOOM",
          "providerMeetingId":"zoom-123",
          "hostStartUrl":"https://zoom.test/start/123",
          "participantJoinUrl":"https://zoom.test/join/123"
        }
        """
            .formatted(sec.getId(), lesson.getId(), startsAt.toString(), endsAt.toString());

    mockMvc
        .perform(
            post("/instructor/courses/{courseId}/live-classes", course.getId())
                .with(authentication(instructorAuth(instructor.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Weekly Session"))
        .andExpect(jsonPath("$.status").value("SCHEDULED"))
        .andExpect(jsonPath("$.timezone").value("Asia/Dhaka"));

    String liveClassId = liveClassRepository.findAll().get(0).getId().toString();

    mockMvc
        .perform(
            post("/instructor/live-classes/{liveClassId}/start", liveClassId)
                .with(authentication(instructorAuth(instructor.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("LIVE"));

    Instant updatedEndsAt = Instant.now().plusSeconds(7200);
    String patchBody =
        """
        {"title":"Updated Session","endsAt":"%s","status":"SCHEDULED"}
        """
            .formatted(updatedEndsAt.toString());
    mockMvc
        .perform(
            patch("/instructor/live-classes/{liveClassId}", liveClassId)
                .with(authentication(instructorAuth(instructor.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(patchBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Updated Session"));

    mockMvc
        .perform(
            delete("/instructor/live-classes/{liveClassId}", liveClassId)
                .with(authentication(instructorAuth(instructor.getId()))))
        .andExpect(status().isOk());
  }

  @Test
  void startDeleteAndCreateEnforceOwnershipAndStatusRules() throws Exception {
    var creator = user("Creator", "creator-inst-live2@example.com");
    var instructor = user("Instructor", "inst-live2@example.com");
    final var otherInstructor = user("Other Instructor", "other-inst-live2@example.com");
    var course = course("Course Two", "course-two-inst", creator, PublishStatus.PUBLISHED);
    assignment(course, instructor, InstructorRole.PRIMARY);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson = lesson(course, sec, 1, PublishStatus.PUBLISHED);
    var completed =
        liveClass(
            course,
            sec,
            lesson,
            instructor,
            LiveClassStatus.COMPLETED,
            Instant.now().minusSeconds(3600),
            Instant.now().minusSeconds(1800));

    mockMvc
        .perform(
            post("/instructor/live-classes/{liveClassId}/start", completed.getId())
                .with(authentication(instructorAuth(instructor.getId()))))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            delete("/instructor/live-classes/{liveClassId}", completed.getId())
                .with(authentication(instructorAuth(instructor.getId()))))
        .andExpect(status().isForbidden());

    String createBody =
        """
        {
          "sectionId":"%s",
          "lessonId":"%s",
          "title":"No Access",
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
                .with(authentication(instructorAuth(otherInstructor.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody))
        .andExpect(status().isForbidden());
  }
}

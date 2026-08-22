package com.gii.api.instructorapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.InstructorRole;
import com.gii.common.enums.PublishStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class InstructorCourseAnnouncementsApiIt extends AbstractInstructorApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupInstructorData();
  }

  @Test
  void assignedInstructorCanCreateAnnouncement() throws Exception {
    var instructor = user("Assigned Instructor", "announcement-instructor@example.com");
    var course = course("Course A", "announcement-course-a", instructor, PublishStatus.PUBLISHED);
    assignment(course, instructor, InstructorRole.PRIMARY);

    mockMvc
        .perform(
            post("/instructor/courses/{courseId}/announcements", course.getId())
                .with(authentication(instructorAuth(instructor.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"title\":\"  Schedule update  \","
                        + "\"content\":\"  Class starts at 10 AM.  \"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.courseId").value(course.getId().toString()))
        .andExpect(jsonPath("$.courseTitle").value("Course A"))
        .andExpect(jsonPath("$.title").value("Schedule update"))
        .andExpect(jsonPath("$.content").value("Class starts at 10 AM."));

    var saved = courseAnnouncementRepository.findAll().getFirst();
    assertThat(saved.getCreatedBy().getId()).isEqualTo(instructor.getId());
  }

  @Test
  void unassignedInstructorCannotCreateAnnouncementButAdminCan() throws Exception {
    var creator = user("Creator", "announcement-creator@example.com");
    var outsider = user("Other Instructor", "announcement-outsider@example.com");
    var admin = user("Admin", "announcement-admin@example.com");
    var course = course("Course B", "announcement-course-b", creator, PublishStatus.PUBLISHED);
    String body = "{\"title\":\"Notice\",\"content\":\"Important information\"}";

    mockMvc
        .perform(
            post("/instructor/courses/{courseId}/announcements", course.getId())
                .with(authentication(instructorAuth(outsider.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/instructor/courses/{courseId}/announcements", course.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated());
  }

  @Test
  void rejectsBlankAnnouncementFields() throws Exception {
    var instructor = user("Instructor", "announcement-validation@example.com");
    var course = course("Course C", "announcement-course-c", instructor, PublishStatus.PUBLISHED);
    assignment(course, instructor, InstructorRole.PRIMARY);

    mockMvc
        .perform(
            post("/instructor/courses/{courseId}/announcements", course.getId())
                .with(authentication(instructorAuth(instructor.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\" \",\"content\":\" \"}"))
        .andExpect(status().isBadRequest());
  }
}

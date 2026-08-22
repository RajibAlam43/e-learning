package com.gii.api.studentapi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class StudentCourseAnnouncementsApiIt extends AbstractStudentApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupStudentData();
  }

  @Test
  void listsNewestAnnouncementsOnlyForActiveUnexpiredEnrollments() throws Exception {
    var author = user("Author", "student-announcement-author@example.com");
    var student = user("Student", "student-announcement-reader@example.com");
    var activeCourse =
        course("Active Course", "announcement-active", author, PublishStatus.PUBLISHED);
    var expiredCourse =
        course("Expired Course", "announcement-expired", author, PublishStatus.PUBLISHED);
    var revokedCourse =
        course("Revoked Course", "announcement-revoked", author, PublishStatus.PUBLISHED);
    final var otherCourse =
        course("Other Course", "announcement-other", author, PublishStatus.PUBLISHED);

    enrollment(student, activeCourse, EnrollmentStatus.ACTIVE, null);
    enrollment(student, expiredCourse, EnrollmentStatus.ACTIVE, Instant.now().minusSeconds(60));
    enrollment(student, revokedCourse, EnrollmentStatus.REVOKED, null);

    announcement(activeCourse, author, "Older visible", "First visible announcement");
    announcement(activeCourse, author, "Newest visible", "Second visible announcement");
    announcement(expiredCourse, author, "Expired hidden", "Not visible");
    announcement(revokedCourse, author, "Revoked hidden", "Not visible");
    announcement(otherCourse, author, "Unenrolled hidden", "Not visible");

    mockMvc
        .perform(
            get("/student/announcements")
                .param("size", "100")
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].title").value("Newest visible"))
        .andExpect(jsonPath("$.content[0].courseTitle").value("Active Course"))
        .andExpect(jsonPath("$.content[1].title").value("Older visible"))
        .andExpect(jsonPath("$.size").value(50))
        .andExpect(jsonPath("$.totalElements").value(2));
  }

  @Test
  void returnsEmptyPageWhenStudentHasNoActiveEnrollments() throws Exception {
    var student = user("Student", "student-no-announcements@example.com");

    mockMvc
        .perform(get("/student/announcements").with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(0))
        .andExpect(jsonPath("$.totalElements").value(0));
  }
}

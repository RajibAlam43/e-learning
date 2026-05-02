package com.gii.api.studentapi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.LiveClassRegistrantStatus;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.enums.PublishStatus;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Intent-first contract gap test: joining a COMPLETED class should be denied for live-session
 * joins. Current implementation allows COMPLETED status.
 */
class StudentApiContractGapIt extends AbstractStudentApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupStudentData();
  }

  @Test
  void joinCompletedClassShouldBeForbiddenByContract() throws Exception {
    var student = user("Student Gap", "student-gap@example.com");
    var instructor = user("Instructor Gap", "instructor-gap@example.com");
    var creator = user("Creator", "creator-gap@example.com");
    var course = course("Course Gap", "course-gap", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson = lesson(course, sec, 1, PublishStatus.PUBLISHED, false);
    enrollment(student, course, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(7200));
    var completed =
        liveClass(
            course,
            sec,
            lesson,
            instructor,
            LiveClassStatus.COMPLETED,
            Instant.now().minusSeconds(7200),
            Instant.now().minusSeconds(3600),
            "https://meet.test/completed");
    registrant(student, completed, LiveClassRegistrantStatus.APPROVED);

    mockMvc
        .perform(
            post("/student/live-classes/{liveClassId}/join", completed.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isForbidden());
  }
}

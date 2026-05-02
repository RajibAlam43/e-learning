package com.gii.api.studentapi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class StudentDashboardApiIt extends AbstractStudentApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupStudentData();
  }

  @Test
  void returnsDashboardAggregatesForStudent() throws Exception {
    var student = user("Student One", "student1@example.com");
    profile(student, "https://cdn.test/a.png");
    var creator = user("Creator", "creator-stu1@example.com");
    var course = course("Course A", "course-a", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var l1 = lesson(course, sec, 1, PublishStatus.PUBLISHED, false);
    var l2 = lesson(course, sec, 2, PublishStatus.PUBLISHED, false);
    enrollment(student, course, EnrollmentStatus.ACTIVE, null);
    var progress = completedProgress(student, l1);
    orderItem(
        order(student, com.gii.common.enums.OrderStatus.PAID, BigDecimal.valueOf(1200)),
        course,
        BigDecimal.valueOf(1200),
        BigDecimal.ZERO);
    certificate(student, course, "CERT-001", false);

    mockMvc
        .perform(get("/student/dashboard").with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fullName").value("Student One"))
        .andExpect(jsonPath("$.totalEnrolledCourses").value(1))
        .andExpect(jsonPath("$.totalEarnedCertificates").value(1))
        .andExpect(jsonPath("$.ongoingCourses.length()").value(1))
        .andExpect(jsonPath("$.ongoingCourses[0].completedLessons").value(1))
        .andExpect(jsonPath("$.ongoingCourses[0].totalLessons").value(2))
        .andExpect(
            jsonPath("$.lastLearningActivityAt")
                .value(progress.getUpdatedAt().truncatedTo(ChronoUnit.MICROS).toString()));
  }
}

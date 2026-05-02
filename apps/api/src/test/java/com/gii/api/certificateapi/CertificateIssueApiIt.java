package com.gii.api.certificateapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.api.service.storage.R2PresignedUrlService;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

class CertificateIssueApiIt extends AbstractCertificateApiIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private R2PresignedUrlService r2PresignedUrlService;

  @AfterEach
  void cleanup() {
    cleanupCertificateData();
  }

  @Test
  void issueCertificateCreatesCertificateForEligibleStudentAndIsIdempotent() throws Exception {
    var creator = user("Creator", "creator-cert-1@example.com");
    var instructor = user("Instructor", "instructor-cert-1@example.com");
    var student = user("Student", "student-cert-1@example.com");
    var course = course("Course Cert", "course-cert", creator, PublishStatus.PUBLISHED);
    primaryInstructor(course, instructor);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson1 = lesson(course, sec, 1, PublishStatus.PUBLISHED);
    var lesson2 = lesson(course, sec, 2, PublishStatus.PUBLISHED);
    enrollment(student, course, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(3600));
    completedProgress(student, lesson1);
    completedProgress(student, lesson2);

    mockMvc
        .perform(
            post("/student/courses/{courseId}/certificate", course.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recipientName").value(student.getFullName()))
        .andExpect(jsonPath("$.courseSlug").value("course-cert"))
        .andExpect(jsonPath("$.wasEligible").value(true))
        .andExpect(jsonPath("$.eligibilityReason").value("COURSE_COMPLETED"));

    assertThat(certificateRepository.findByUserIdAndCourseId(student.getId(), course.getId()))
        .isPresent();

    mockMvc
        .perform(
            post("/student/courses/{courseId}/certificate", course.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eligibilityReason").value("CERTIFICATE_ALREADY_EXISTS"));
  }

  @Test
  void issueCertificateRejectsWhenCompletionCriteriaNotMet() throws Exception {
    var creator = user("Creator", "creator-cert-2@example.com");
    var student = user("Student", "student-cert-2@example.com");
    var course = course("Course Cert 2", "course-cert-2", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    lesson(course, sec, 1, PublishStatus.PUBLISHED);
    lesson(course, sec, 2, PublishStatus.PUBLISHED);
    enrollment(student, course, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(3600));

    mockMvc
        .perform(
            post("/student/courses/{courseId}/certificate", course.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isForbidden());
  }
}

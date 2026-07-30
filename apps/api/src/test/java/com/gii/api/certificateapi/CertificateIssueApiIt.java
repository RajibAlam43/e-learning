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
    var course = course("বাংলা কোর্স", "course-cert", creator, PublishStatus.PUBLISHED);
    course.setTitleEn("English Certificate Course");
    courseRepository.saveAndFlush(course);
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
        .andExpect(jsonPath("$.targetType").value("COURSE"))
        .andExpect(jsonPath("$.targetSlug").value("course-cert"))
        .andExpect(jsonPath("$.wasEligible").value(true))
        .andExpect(jsonPath("$.eligibilityReason").value("COURSE_COMPLETED"));

    assertThat(
            certificateRepository
                .findByUserIdAndCourseId(student.getId(), course.getId())
                .orElseThrow()
                .getTargetTitle())
        .isEqualTo("English Certificate Course");

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

  @Test
  void issueCollectionCertificateCreatesCertificateForEligibleStudentAndIsIdempotent()
      throws Exception {
    var creator = user("Creator", "creator-cert-collection@example.com");
    var student = user("Student", "student-cert-collection@example.com");

    var collection =
        collection("Collection Cert", "collection-cert", creator, PublishStatus.PUBLISHED);
    var course1 = course("Collection Course 1", "collection-course-1", creator, PublishStatus.PUBLISHED);
    var course2 = course("Collection Course 2", "collection-course-2", creator, PublishStatus.PUBLISHED);
    collectionCourse(collection, course1, 1, true);
    collectionCourse(collection, course2, 2, true);
    collectionEnrollment(student, collection, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(3600));

    var s1 = section(course1, 1, PublishStatus.PUBLISHED);
    var c1l1 = lesson(course1, s1, 1, PublishStatus.PUBLISHED);
    var c1l2 = lesson(course1, s1, 2, PublishStatus.PUBLISHED);
    var s2 = section(course2, 1, PublishStatus.PUBLISHED);
    var c2l1 = lesson(course2, s2, 1, PublishStatus.PUBLISHED);

    completedProgress(student, c1l1);
    completedProgress(student, c1l2);
    completedProgress(student, c2l1);

    mockMvc
        .perform(
            post("/student/collections/{collectionId}/certificate", collection.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.targetType").value("COLLECTION"))
        .andExpect(jsonPath("$.targetSlug").value("collection-cert"))
        .andExpect(jsonPath("$.wasEligible").value(true))
        .andExpect(jsonPath("$.eligibilityReason").value("COLLECTION_COMPLETED"));

    assertThat(
            certificateRepository.findByUserIdAndCollectionId(student.getId(), collection.getId()))
        .isPresent();

    mockMvc
        .perform(
            post("/student/collections/{collectionId}/certificate", collection.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eligibilityReason").value("CERTIFICATE_ALREADY_EXISTS"));
  }
}

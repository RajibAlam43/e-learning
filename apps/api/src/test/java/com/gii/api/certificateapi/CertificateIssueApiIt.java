package com.gii.api.certificateapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.api.service.storage.R2ObjectStorageService;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

class CertificateIssueApiIt extends AbstractCertificateApiIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private R2ObjectStorageService objectStorageService;

  @BeforeEach
  void mockCertificateUpload() {
    when(objectStorageService.bucket()).thenReturn("test-bucket");
    when(objectStorageService.storageLocation(any()))
        .thenAnswer(invocation -> "r2://test-bucket/" + invocation.getArgument(0));
  }

  @AfterEach
  void cleanup() {
    cleanupCertificateData();
  }

  @Test
  void issueCertificateCreatesCertificateForEligibleStudentAndIsIdempotent() throws Exception {
    var creator = user("Creator", "creator-cert-1@example.com");
    var instructor = user("Instructor", "instructor-cert-1@example.com");
    final var student = user("Student", "student-cert-1@example.com");
    var course = course("বাংলা কোর্স", "course-cert", creator, PublishStatus.PUBLISHED);
    course.setTitleEn("English Certificate Course");
    courseRepository.saveAndFlush(course);
    primaryInstructor(course, instructor);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson1 = lesson(course, sec, 1, PublishStatus.PUBLISHED);
    var lesson2 = lesson(course, sec, 2, PublishStatus.PUBLISHED);
    var courseEnrollment =
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
        .andExpect(
            jsonPath("$.objectKey")
                .value(
                    "certificates/"
                        + student.getId()
                        + "/course-enrollments/"
                        + courseEnrollment.getId()
                        + ".pdf"))
        .andExpect(jsonPath("$.wasEligible").value(true))
        .andExpect(jsonPath("$.eligibilityReason").value("COURSE_COMPLETED"));

    assertThat(
            certificateRepository
                .findByUserIdAndCourseId(student.getId(), course.getId())
                .orElseThrow()
                .getTargetTitle())
        .isEqualTo("English Certificate Course");
    var savedCertificate =
        certificateRepository
            .findByUserIdAndCourseId(student.getId(), course.getId())
            .orElseThrow();
    assertThat(savedCertificate.getPdfObjectKey())
        .isEqualTo(
            "certificates/"
                + student.getId()
                + "/course-enrollments/"
                + courseEnrollment.getId()
                + ".pdf");
    assertThat(savedCertificate.getTemplate()).isNotNull();
    assertThat(savedCertificate.getInstructorName()).isEqualTo(instructor.getFullName());
    verify(objectStorageService)
        .put(
            eq(savedCertificate.getPdfObjectKey()),
            argThat(
                pdf ->
                    pdf != null
                        && pdf.length > 5
                        && pdf[0] == '%'
                        && pdf[1] == 'P'
                        && pdf[2] == 'D'
                        && pdf[3] == 'F'),
            eq("application/pdf"));

    instructor.setFullName("Renamed Instructor");
    userRepository.saveAndFlush(instructor);
    mockMvc
        .perform(
            get("/student/certificates/{certificateId}", savedCertificate.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.instructorName").value("Instructor"));

    mockMvc
        .perform(
            post("/student/courses/{courseId}/certificate", course.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eligibilityReason").value("CERTIFICATE_ALREADY_EXISTS"));
    verify(objectStorageService, times(1)).put(any(), any(), any());
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
  void revokedCertificateCannotBeReissuedByStudent() throws Exception {
    var creator = user("Creator", "creator-cert-revoked@example.com");
    var student = user("Student", "student-cert-revoked@example.com");
    var course = course("Revoked Cert", "revoked-cert", creator, PublishStatus.PUBLISHED);
    enrollment(student, course, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(3600));
    certificate(
        student, course, "GII-CERT-REVOKED1", true, "https://cdn.test/revoked.pdf", creator);

    mockMvc
        .perform(
            post("/student/courses/{courseId}/certificate", course.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.detail").value("Certificate has been revoked"));

    assertThat(certificateRepository.findByUserIdAndCourseId(student.getId(), course.getId()))
        .get()
        .extracting(com.gii.common.entity.certificate.Certificate::getRevokedAt)
        .isNotNull();
  }

  @Test
  void issueCollectionCertificateCreatesCertificateForEligibleStudentAndIsIdempotent()
      throws Exception {
    var creator = user("Creator", "creator-cert-collection@example.com");
    var student = user("Student", "student-cert-collection@example.com");

    var collection =
        collection("Collection Cert", "collection-cert", creator, PublishStatus.PUBLISHED);
    var course1 =
        course("Collection Course 1", "collection-course-1", creator, PublishStatus.PUBLISHED);
    var course2 =
        course("Collection Course 2", "collection-course-2", creator, PublishStatus.PUBLISHED);
    collectionCourse(collection, course1, 1, true);
    collectionCourse(collection, course2, 2, true);
    collectionEnrollment(
        student, collection, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(3600));

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

  @Test
  void collectionCertificateDoesNotIgnorePurchasedCourseThatBecameUnpublished() throws Exception {
    var creator = user("Creator", "creator-cert-unpublished@example.com");
    var student = user("Student", "student-cert-unpublished@example.com");
    var collection =
        collection("Stable Collection", "stable-collection", creator, PublishStatus.PUBLISHED);
    var completedCourse =
        course("Completed Course", "completed-course", creator, PublishStatus.PUBLISHED);
    var unpublishedCourse =
        course("Unpublished Course", "unpublished-course", creator, PublishStatus.DRAFT);
    collectionCourse(collection, completedCourse, 1, true);
    collectionCourse(collection, unpublishedCourse, 2, true);
    collectionEnrollment(student, collection, EnrollmentStatus.ACTIVE, null);

    var completedSection = section(completedCourse, 1, PublishStatus.PUBLISHED);
    var completedLesson = lesson(completedCourse, completedSection, 1, PublishStatus.PUBLISHED);
    var unpublishedSection = section(unpublishedCourse, 1, PublishStatus.PUBLISHED);
    lesson(unpublishedCourse, unpublishedSection, 1, PublishStatus.PUBLISHED);
    completedProgress(student, completedLesson);

    mockMvc
        .perform(
            post("/student/collections/{collectionId}/certificate", collection.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isForbidden());

    assertThat(
            certificateRepository.findByUserIdAndCollectionId(student.getId(), collection.getId()))
        .isEmpty();
  }

  @Test
  void storageFailureRollsBackCertificateRow() throws Exception {
    var creator = user("Creator", "creator-cert-storage-failure@example.com");
    var student = user("Student", "student-cert-storage-failure@example.com");
    var course = course("Storage Failure", "storage-failure", creator, PublishStatus.PUBLISHED);
    var section = section(course, 1, PublishStatus.PUBLISHED);
    var lesson = lesson(course, section, 1, PublishStatus.PUBLISHED);
    enrollment(student, course, EnrollmentStatus.ACTIVE, null);
    completedProgress(student, lesson);
    doThrow(
            new ResponseStatusException(
                HttpStatus.BAD_GATEWAY, "Certificate storage is temporarily unavailable"))
        .when(objectStorageService)
        .put(any(), any(), any());

    mockMvc
        .perform(
            post("/student/courses/{courseId}/certificate", course.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isBadGateway());

    assertThat(certificateRepository.findByUserIdAndCourseId(student.getId(), course.getId()))
        .isEmpty();
  }

  @Test
  void simultaneousCertificateGenerationCreatesAndUploadsOnce() throws Exception {
    var creator = user("Creator", "creator-cert-concurrent@example.com");
    var student = user("Student", "student-cert-concurrent@example.com");
    var course = course("Concurrent", "concurrent", creator, PublishStatus.PUBLISHED);
    var section = section(course, 1, PublishStatus.PUBLISHED);
    var lesson = lesson(course, section, 1, PublishStatus.PUBLISHED);
    enrollment(student, course, EnrollmentStatus.ACTIVE, null);
    completedProgress(student, lesson);
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    var executor = Executors.newFixedThreadPool(2);
    try {
      var request =
          (java.util.concurrent.Callable<Integer>)
              () -> {
                ready.countDown();
                start.await();
                return mockMvc
                    .perform(
                        post("/student/courses/{courseId}/certificate", course.getId())
                            .with(authentication(studentAuth(student.getId()))))
                    .andReturn()
                    .getResponse()
                    .getStatus();
              };
      var first = executor.submit(request);
      var second = executor.submit(request);
      ready.await();
      start.countDown();

      assertThat(first.get()).isEqualTo(200);
      assertThat(second.get()).isEqualTo(200);
    } finally {
      executor.shutdownNow();
    }

    assertThat(certificateRepository.findByUserIdAndCourseId(student.getId(), course.getId()))
        .isPresent();
    assertThat(certificateRepository.count()).isEqualTo(1);
    verify(objectStorageService, times(1)).put(any(), any(), any());
  }
}

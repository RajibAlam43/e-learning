package com.gii.api.certificateapi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.api.service.storage.R2PresignedUrlService;
import com.gii.common.enums.PublishStatus;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

class CertificateDownloadAndVerifyApiIt extends AbstractCertificateApiIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private R2PresignedUrlService r2PresignedUrlService;

  @AfterEach
  void cleanup() {
    cleanupCertificateData();
  }

  @Test
  void downloadReturnsSignedUrlForOwnerAndBlocksRevoked() throws Exception {
    var creator = user("Creator", "creator-cert-3@example.com");
    var instructor = user("Instructor", "instructor-cert-3@example.com");
    var student = user("Student", "student-cert-3@example.com");
    var courseA = course("Course Cert 3A", "course-cert-3a", creator, PublishStatus.PUBLISHED);
    var courseB = course("Course Cert 3B", "course-cert-3b", creator, PublishStatus.PUBLISHED);
    primaryInstructor(courseA, instructor);
    primaryInstructor(courseB, instructor);
    var valid =
        certificate(
            student,
            courseA,
            "GII-CERT-ABCDEF1234",
            false,
            "https://cdn.test/cert.pdf",
            instructor);
    var revoked =
        certificate(
            student,
            courseB,
            "GII-CERT-REV1234567",
            true,
            "https://cdn.test/cert2.pdf",
            instructor);
    when(r2PresignedUrlService.generateDownloadUrl(any(), any(), any()))
        .thenReturn(
            new R2PresignedUrlService.PresignedDownload(
                "https://signed.test/cert", Instant.now().plusSeconds(600)));

    mockMvc
        .perform(
            get("/student/certificates/{certificateId}/download", valid.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.downloadUrl").value("https://signed.test/cert"))
        .andExpect(jsonPath("$.contentType").value("application/pdf"));

    mockMvc
        .perform(
            get("/student/certificates/{certificateId}/download", revoked.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isForbidden());
  }

  @Test
  void verifyReturnsValidAndRevokedStates() throws Exception {
    var creator = user("Creator", "creator-cert-4@example.com");
    var instructor = user("Instructor", "instructor-cert-4@example.com");
    var student = user("Student", "student-cert-4@example.com");
    var courseA = course("Course Cert 4A", "course-cert-4a", creator, PublishStatus.PUBLISHED);
    var courseB = course("Course Cert 4B", "course-cert-4b", creator, PublishStatus.PUBLISHED);
    primaryInstructor(courseA, instructor);
    primaryInstructor(courseB, instructor);
    var secA = section(courseA, 1, PublishStatus.PUBLISHED);
    var lessonA = lesson(courseA, secA, 1, PublishStatus.PUBLISHED);
    completedProgress(student, lessonA);
    var valid =
        certificate(
            student,
            courseA,
            "GII-CERT-VALID1111",
            false,
            "https://cdn.test/cert4.pdf",
            instructor);
    var revoked =
        certificate(
            student, courseB, "GII-CERT-REVOKE111", true, "https://cdn.test/cert5.pdf", instructor);

    mockMvc
        .perform(get("/public/certificates/verify/{code}", valid.getCertificateCode()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isValid").value(true))
        .andExpect(jsonPath("$.verificationStatus").value("VALID"));

    mockMvc
        .perform(
            get("/public/certificates/verify/{code}", valid.getCertificateCode().toLowerCase()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isValid").value(true))
        .andExpect(jsonPath("$.certificateCode").value(valid.getCertificateCode()));

    mockMvc
        .perform(get("/public/certificates/verify/{code}", revoked.getCertificateCode()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isValid").value(false))
        .andExpect(jsonPath("$.verificationStatus").value("REVOKED"));
  }
}

package com.gii.api.certificateapi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.PublishStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class CertificateApiContractGapIt extends AbstractCertificateApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupCertificateData();
  }

  @Test
  void downloadingOthersCertificateShouldBeForbiddenByContract() throws Exception {
    var creator = user("Creator", "creator-cert-gap@example.com");
    var issuer = user("Issuer", "issuer-cert-gap@example.com");
    var owner = user("Owner", "owner-cert-gap@example.com");
    var attacker = user("Attacker", "attacker-cert-gap@example.com");
    var course = course("Course Gap", "course-gap-cert", creator, PublishStatus.PUBLISHED);
    var cert =
        certificate(owner, course, "GII-CERT-GAP00001", false, "https://cdn.test/gap.pdf", issuer);

    mockMvc
        .perform(
            get("/student/certificates/{certificateId}/download", cert.getId())
                .with(authentication(studentAuth(attacker.getId()))))
        .andExpect(status().isForbidden());
  }
}

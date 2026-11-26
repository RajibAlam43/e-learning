package com.gii.worker.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gii.common.dto.EmailJobMessage;
import com.gii.common.enums.VerificationPurpose;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class VerificationEmailTemplateServiceTest {
  private final VerificationEmailTemplateService templates = new VerificationEmailTemplateService();

  @Test
  void rendersLeadingZeroCodeAndActualExpiry() throws IOException {
    String html =
        templates.render(
            EmailJobMessage.builder()
                .verificationPurpose(VerificationPurpose.EMAIL_VERIFICATION)
                .verificationCode("012345")
                .expiresAt(Instant.parse("2026-11-26T12:15:00Z"))
                .build());

    assertThat(html)
        .contains("012345", "26 Nov 2026, 12:15 UTC", "Verify your account")
        .doesNotContain("{{", "1 hour", "Gyanify");
    Path output = Path.of("build/email-previews/verification.html");
    Files.createDirectories(output.getParent());
    Files.writeString(output, html);
  }

  @Test
  void escapesLegacyContentAndDoesNotInterpretInjectedTemplateMarkers() {
    String html =
        templates.render(
            EmailJobMessage.builder()
                .verificationPurpose(VerificationPurpose.PASSWORD_RESET)
                .body("<img src=x onerror=alert(1)> & {{TITLE}}")
                .build());

    assertThat(html)
        .contains("Reset your password", "&lt;img", "&amp; {{TITLE}}")
        .doesNotContain("<img src=x", "YOUR 6-DIGIT CODE");
  }

  @Test
  void olderQueuedMessagesRetainTheirExpiryWording() throws IOException {
    var mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    var oldJob =
        mapper.readValue(
            """
            {"verificationCode":"654321","body":"Your code expires in 7 minutes.",
             "verificationPurpose":"EMAIL_VERIFICATION"}
            """,
            EmailJobMessage.class);

    assertThat(oldJob.expiresAt()).isNull();
    assertThat(templates.render(oldJob)).contains("654321", "7 minutes");
  }
}

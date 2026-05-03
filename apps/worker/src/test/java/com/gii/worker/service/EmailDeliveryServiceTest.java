package com.gii.worker.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.gii.common.dto.EmailJobMessage;
import com.gii.common.enums.EmailJobType;
import com.gii.common.enums.VerificationPurpose;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

@ExtendWith(MockitoExtension.class)
class EmailDeliveryServiceTest {

  @Mock private SesClient sesClient;

  @InjectMocks private EmailDeliveryService emailDeliveryService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(emailDeliveryService, "fromDomain", "notifications.example.com");
    ReflectionTestUtils.setField(emailDeliveryService, "fromLocalPart", "no-reply");
  }

  @Test
  void send_shouldBuildAndSendSesRequest() {
    EmailJobMessage job =
        EmailJobMessage.builder()
            .userId(UUID.randomUUID())
            .jobType(EmailJobType.OTP_VERIFICATION)
            .toEmail("student@example.com")
            .subject("Verify email")
            .body("Your OTP is 123456")
            .verificationPurpose(VerificationPurpose.EMAIL_VERIFICATION)
            .createdAt(Instant.now())
            .build();

    emailDeliveryService.send(job);

    ArgumentCaptor<SendEmailRequest> requestCaptor =
        ArgumentCaptor.forClass(SendEmailRequest.class);
    verify(sesClient).sendEmail(requestCaptor.capture());
    SendEmailRequest request = requestCaptor.getValue();

    org.assertj.core.api.Assertions.assertThat(request.source())
        .isEqualTo("no-reply@notifications.example.com");
    org.assertj.core.api.Assertions.assertThat(request.destination().toAddresses())
        .containsExactly("student@example.com");
    org.assertj.core.api.Assertions.assertThat(request.message().subject().data())
        .isEqualTo("Verify email");
    org.assertj.core.api.Assertions.assertThat(request.message().body().text().data())
        .isEqualTo("Your OTP is 123456");
  }

  @Test
  void send_whenRecipientMissing_shouldNotCallSes() {
    EmailJobMessage job =
        EmailJobMessage.builder()
            .userId(UUID.randomUUID())
            .jobType(EmailJobType.OTP_VERIFICATION)
            .toEmail(" ")
            .subject("Verify email")
            .body("Your OTP is 123456")
            .build();

    emailDeliveryService.send(job);

    verify(sesClient, never()).sendEmail(any(SendEmailRequest.class));
  }

  @Test
  void send_whenFromDomainMissing_shouldUseFallbackSource() {
    ReflectionTestUtils.setField(emailDeliveryService, "fromDomain", "");
    EmailJobMessage job =
        EmailJobMessage.builder()
            .userId(UUID.randomUUID())
            .jobType(EmailJobType.OTP_VERIFICATION)
            .toEmail("student@example.com")
            .subject("Verify email")
            .body("Your OTP is 123456")
            .build();

    emailDeliveryService.send(job);

    ArgumentCaptor<SendEmailRequest> requestCaptor =
        ArgumentCaptor.forClass(SendEmailRequest.class);
    verify(sesClient).sendEmail(requestCaptor.capture());
    org.assertj.core.api.Assertions.assertThat(requestCaptor.getValue().source())
        .isEqualTo("no-reply@globalislamicinstitute.com");
  }
}

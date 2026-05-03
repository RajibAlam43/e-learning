package com.gii.worker.listener;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.gii.common.dto.EmailJobMessage;
import com.gii.common.enums.EmailJobType;
import com.gii.worker.service.EmailDeliveryService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobsListenerTest {

  @Mock private EmailDeliveryService emailDeliveryService;

  @InjectMocks private JobsListener jobsListener;

  @Test
  void receiveEmailJobs_shouldDelegateToDeliveryService() {
    EmailJobMessage job =
        EmailJobMessage.builder()
            .userId(UUID.randomUUID())
            .jobType(EmailJobType.OTP_VERIFICATION)
            .toEmail("user@example.com")
            .subject("Verify your account")
            .body("OTP: 123456")
            .build();

    jobsListener.receiveEmailJobs(job);

    verify(emailDeliveryService).send(job);
  }

  @Test
  void receiveEmailJobs_whenDeliveryFails_shouldThrowIllegalStateException() {
    EmailJobMessage job =
        EmailJobMessage.builder()
            .userId(UUID.randomUUID())
            .jobType(EmailJobType.OTP_VERIFICATION)
            .toEmail("user@example.com")
            .subject("Verify your account")
            .body("OTP: 123456")
            .build();
    RuntimeException rootCause = new RuntimeException("send failed");
    doThrow(rootCause).when(emailDeliveryService).send(job);

    assertThatThrownBy(() -> jobsListener.receiveEmailJobs(job))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Unable to process email job message")
        .hasCause(rootCause);
  }
}


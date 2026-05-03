package com.gii.worker.service;

import com.gii.common.dto.EmailJobMessage;
import com.gii.common.enums.EmailJobType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailDeliveryService {

  private final SesClient sesClient;

  @Value("${aws.ses.from-domain:}")
  private String fromDomain;

  @Value("${aws.ses.from-local-part:no-reply}")
  private String fromLocalPart;

  public void send(EmailJobMessage job) {
    if (job.jobType() != EmailJobType.OTP_VERIFICATION) {
      log.warn("Unsupported email job type {} for user {}", job.jobType(), job.userId());
      return;
    }
    if (job.toEmail() == null || job.toEmail().isBlank()) {
      log.warn("Skipping email job without recipient for user {}", job.userId());
      return;
    }

    SendEmailRequest request =
        SendEmailRequest.builder()
            .source(resolveFromAddress())
            .destination(Destination.builder().toAddresses(job.toEmail()).build())
            .message(
                Message.builder()
                    .subject(Content.builder().data(job.subject()).charset("UTF-8").build())
                    .body(
                        Body.builder()
                            .text(Content.builder().data(job.body()).charset("UTF-8").build())
                            .build())
                    .build())
            .build();

    sesClient.sendEmail(request);
  }

  private String resolveFromAddress() {
    if (!fromDomain.isBlank()) {
      return fromLocalPart + "@" + fromDomain;
    }
    return "no-reply@globalislamicinstitute.com";
  }
}

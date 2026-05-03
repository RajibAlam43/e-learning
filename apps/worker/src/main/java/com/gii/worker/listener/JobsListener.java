package com.gii.worker.listener;

import com.gii.common.dto.EmailJobMessage;
import com.gii.worker.service.EmailDeliveryService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobsListener {

  private final EmailDeliveryService emailDeliveryService;

  @SqsListener(value = "${email.jobs.main.queue}")
  public void receiveEmailJobs(EmailJobMessage job) {
    try {
      emailDeliveryService.send(job);
      log.info("Processed email job type={} to={}", job.jobType(), job.toEmail());
    } catch (Exception e) {
      log.error("Failed to process SQS email job payload for recipient {}", job.toEmail(), e);
      throw new IllegalStateException("Unable to process email job message", e);
    }
  }
}

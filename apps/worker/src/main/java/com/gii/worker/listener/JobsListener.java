package com.gii.worker.listener;

import com.gii.common.dto.EmailJobMessage;
import com.gii.common.dto.SslcommerzValidationJobMessage;
import com.gii.common.dto.SmsJobMessage;
import com.gii.worker.service.EmailDeliveryService;
import com.gii.worker.service.SslcommerzValidationJobService;
import com.gii.worker.service.SmsDeliveryService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobsListener {

  private final EmailDeliveryService emailDeliveryService;
  private final SmsDeliveryService smsDeliveryService;
  private final SslcommerzValidationJobService sslcommerzValidationJobService;

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

  @SqsListener(value = "${sms.jobs.main.queue}")
  public void receiveSmsJobs(SmsJobMessage job) {
    try {
      smsDeliveryService.sendOtp(job);
      log.info("Processed sms OTP job to={}", job.toPhoneNumber());
    } catch (Exception e) {
      log.error("Failed to process SQS SMS job payload for recipient {}", job.toPhoneNumber(), e);
      throw new IllegalStateException("Unable to process SMS job message", e);
    }
  }

  @SqsListener(value = "${payments.sslcommerz.validation.jobs.queue}")
  public void receiveSslcommerzValidationJobs(SslcommerzValidationJobMessage job) {
    try {
      sslcommerzValidationJobService.process(job);
      log.info(
          "Processed SSLCommerz validation job orderId={} valId={} attempt={}/{}",
          job.orderId(),
          job.valId(),
          job.attempt(),
          job.maxAttempts());
    } catch (Exception e) {
      log.error(
          "Failed to process SSLCommerz validation job orderId={} valId={} attempt={}/{}",
          job.orderId(),
          job.valId(),
          job.attempt(),
          job.maxAttempts(),
          e);
      throw new IllegalStateException("Unable to process SSLCommerz validation job message", e);
    }
  }
}

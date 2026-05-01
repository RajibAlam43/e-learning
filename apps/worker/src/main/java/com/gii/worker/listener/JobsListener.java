package com.gii.worker.listener;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class JobsListener {

  public JobsListener() {
    log.info("JobsListener bean created");
  }

  @SqsListener(value = "${email.jobs.main.queue}")
  public void receiveEmailJobs(Message<@NotNull String> lifeContractPayload) {
    log.info("Message payload: {}", lifeContractPayload);
  }
}

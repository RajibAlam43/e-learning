package com.gii.worker.listener;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import static io.awspring.cloud.sqs.listener.SqsHeaders.MessageSystemAttributes.MESSAGE_ID;

@Service
@Slf4j
public class JobsListener {

    @SqsListener(value = "${email.jobs.main.queue}")
    public void receiveEmailJobs(Message<@NotNull String> lifeContractPayload, @Header(MESSAGE_ID) String messageId) {
        log.info("Message payload: {}", lifeContractPayload);
        log.info("messageId: {}", messageId);
    }
}
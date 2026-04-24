package com.gii.worker.listener;

import com.gii.common.model.JobExecutionLog;
import com.gii.common.repository.JobExecutionLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SqsListener {

    private static final Logger log = LoggerFactory.getLogger(SqsListener.class);
    private static final String QUEUE_KEY = "jobs:test";

    private final StringRedisTemplate stringRedisTemplate;
    private final JobExecutionLogRepository jobExecutionLogRepository;

    public SqsListener(StringRedisTemplate stringRedisTemplate,
                       JobExecutionLogRepository jobExecutionLogRepository) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.jobExecutionLogRepository = jobExecutionLogRepository;
    }

    @Scheduled(fixedDelay = 3000)
    public void consume() {
        String payload = stringRedisTemplate.opsForList().rightPop(QUEUE_KEY);

        if (payload == null) {
            return;
        }

        log.info("Consumed test job payload: {}", payload);

        String[] parts = payload.split("\\|", 2);
        String jobId = parts[0];
        String message = parts.length > 1 ? parts[1] : "";

        JobExecutionLog logEntry = jobExecutionLogRepository.findByJobId(jobId)
                .orElseGet(() -> JobExecutionLog.builder()
                        .jobId(jobId)
                        .message(message)
                        .build());

        logEntry.setStatus("PROCESSED");
        logEntry.setMessage(message);

        jobExecutionLogRepository.save(logEntry);
    }
}
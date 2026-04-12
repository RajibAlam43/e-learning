package com.gii.api.service;

import com.gii.common.model.JobExecutionLog;
import com.gii.common.repository.JobExecutionLogRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TestJobService {

    private static final String QUEUE_KEY = "jobs:test";

    private final StringRedisTemplate stringRedisTemplate;
    private final JobExecutionLogRepository jobExecutionLogRepository;

    public TestJobService(StringRedisTemplate stringRedisTemplate,
                               JobExecutionLogRepository jobExecutionLogRepository) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.jobExecutionLogRepository = jobExecutionLogRepository;
    }

    public String enqueue(String message) {
        String jobId = UUID.randomUUID().toString();
        String payload = jobId + "|" + message;

        jobExecutionLogRepository.save(
                JobExecutionLog.builder()
                        .jobId(jobId)
                        .status("QUEUED")
                        .message(message)
                        .build()
        );

        stringRedisTemplate.opsForList().leftPush(QUEUE_KEY, payload);
        return jobId;
    }
}
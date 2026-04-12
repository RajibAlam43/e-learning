package com.gii.api.controller;

import com.gii.api.model.TestJobRequest;
import com.gii.api.service.TestJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ELearningApiController {

    private final TestJobService testJobService;

    @Autowired
    public ELearningApiController(TestJobService testJobService) {
        this.testJobService = testJobService;
    }

    @GetMapping("/api/public/ping")
    public String ping() {
        return "pong";
    }

    @PostMapping("/test")
    public Map<String, String> enqueueTestJob(@RequestBody TestJobRequest request) {
        String message = request.getMessage() == null ? "hello" : request.getMessage();
        String jobId = testJobService.enqueue(message);
        return Map.of(
                "status", "queued",
                "jobId", jobId,
                "message", message
        );
    }
}

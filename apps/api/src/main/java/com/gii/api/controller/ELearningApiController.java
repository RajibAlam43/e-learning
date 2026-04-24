package com.gii.api.controller;

import com.gii.api.service.SqsProducerService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ELearningApiController {

    private final SqsProducerService sqsProducerService;

    @Autowired
    public ELearningApiController(SqsProducerService sqsProducerService) {
        this.sqsProducerService = sqsProducerService;
    }

    @GetMapping("ping")
    public String ping() {
        return "pong";
    }

    @PostMapping("/test-sqs")
    public ResponseEntity<@NotNull HttpStatus> enqueueTestJob(@RequestBody String request) {
        sqsProducerService.sendMessage(request, "gii-stage-email-jobs-queue", null);
        return ResponseEntity.ok(HttpStatus.OK);
    }
}

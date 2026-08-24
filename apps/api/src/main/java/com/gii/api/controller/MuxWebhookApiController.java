package com.gii.api.controller;

import com.gii.api.service.media.MuxWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MuxWebhookApiController implements MuxWebhookApi {

  private final MuxWebhookService muxWebhookService;

  @Override
  public ResponseEntity<Void> receiveMuxWebhook(String signature, String rawBody) {
    muxWebhookService.execute(signature, rawBody);
    return ResponseEntity.noContent().build();
  }
}

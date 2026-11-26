package com.gii.api.controller;

import com.gii.api.model.response.live.LiveClassCoHostResponse;
import com.gii.api.service.live.LiveClassCoHostService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LiveClassCoHostApiController implements LiveClassCoHostApi {
  private final LiveClassCoHostService service;

  @Override
  public ResponseEntity<LiveClassCoHostResponse> add(
      UUID liveClassId, AddCoHostRequest request, Authentication authentication) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(service.add(liveClassId, request == null ? null : request.email(), authentication));
  }
}

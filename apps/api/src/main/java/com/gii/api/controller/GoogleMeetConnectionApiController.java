package com.gii.api.controller;

import com.gii.api.service.live.oauth.GoogleMeetConnectionService;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class GoogleMeetConnectionApiController implements GoogleMeetConnectionApi {
  private final GoogleMeetConnectionService service;

  @Override
  public ResponseEntity<GoogleMeetConnectionService.Status> status(Authentication authentication) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(service.status(authentication));
  }

  @Override
  public ResponseEntity<GoogleMeetConnectionService.Authorization> authorize(
      Authentication authentication, HttpServletResponse response) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(service.start(authentication, response));
  }

  @Override
  public ResponseEntity<Void> disconnect(Authentication authentication) {
    service.disconnect(authentication);
    return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
  }

  @Override
  public ResponseEntity<?> callback(
      String state, String code, String error, String browser, HttpServletResponse response) {
    boolean success = false;
    try {
      service.complete(state, code, error, browser);
      success = true;
    } catch (ResponseStatusException failure) {
      if (service.returnUrl(false).isBlank()) {
        throw failure;
      }
    } finally {
      service.clearCookie(response);
    }
    String url = service.returnUrl(success);
    if (!url.isBlank()) {
      return ResponseEntity.status(HttpStatus.SEE_OTHER)
          .cacheControl(CacheControl.noStore())
          .location(URI.create(url))
          .build();
    }
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(Map.of("connected", success));
  }
}

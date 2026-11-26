package com.gii.api.controller;

import com.gii.api.service.live.oauth.GoogleMeetConnectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Teacher Google authorization")
public interface GoogleMeetConnectionApi {
  @GetMapping("/instructor/google-meet/connection")
  @PreAuthorize("hasAnyRole('INSTRUCTOR','TEACHER','ADMIN','SUPER_ADMIN')")
  @Operation(summary = "Get the current user's Google Meet connection and organizer mode")
  ResponseEntity<GoogleMeetConnectionService.Status> status(Authentication authentication);

  @PostMapping("/instructor/google-meet/connection/authorize")
  @PreAuthorize("hasAnyRole('INSTRUCTOR','TEACHER','ADMIN','SUPER_ADMIN')")
  @Operation(summary = "Begin Google consent for the authenticated teacher")
  ResponseEntity<GoogleMeetConnectionService.Authorization> authorize(
      Authentication authentication, HttpServletResponse response);

  @DeleteMapping("/instructor/google-meet/connection")
  @PreAuthorize("hasAnyRole('INSTRUCTOR','TEACHER','ADMIN','SUPER_ADMIN')")
  @Operation(summary = "Delete this user's stored Google authorization")
  ResponseEntity<Void> disconnect(Authentication authentication);

  @GetMapping(GoogleMeetConnectionService.CALLBACK)
  @Operation(summary = "Complete Google consent using a single-use browser-bound state")
  ResponseEntity<?> callback(
      @RequestParam(required = false) String state,
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String error,
      @CookieValue(name = GoogleMeetConnectionService.COOKIE, required = false) String browser,
      HttpServletResponse response);
}

package com.gii.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Mux Webhooks", description = "Signed Mux video lifecycle notifications")
public interface MuxWebhookApi {

  @PostMapping(value = "/webhooks/mux", consumes = "application/json")
  @Operation(
      summary = "Receive Mux video events",
      description = "Verify and reconcile Mux direct-upload and asset lifecycle events.",
      security = {})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Webhook accepted"),
        @ApiResponse(responseCode = "400", description = "Malformed payload"),
        @ApiResponse(responseCode = "401", description = "Invalid or stale signature")
      })
  ResponseEntity<Void> receiveMuxWebhook(
      @RequestHeader(value = "Mux-Signature", required = false) String signature,
      @RequestBody String rawBody);
}

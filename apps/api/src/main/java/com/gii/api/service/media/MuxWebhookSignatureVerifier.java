package com.gii.api.service.media;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class MuxWebhookSignatureVerifier {

  private static final String HMAC_SHA_256 = "HmacSHA256";

  private final byte[] secret;
  private final long toleranceSeconds;

  public MuxWebhookSignatureVerifier(
      @Value("${mux.webhook-secret}") String secret,
      @Value("${mux.webhook-tolerance-seconds:300}") long toleranceSeconds) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException("mux.webhook-secret must be configured");
    }
    if (toleranceSeconds <= 0 || toleranceSeconds > 86400) {
      throw new IllegalStateException("mux.webhook-tolerance-seconds must be between 1 and 86400");
    }
    this.secret = secret.getBytes(StandardCharsets.UTF_8);
    this.toleranceSeconds = toleranceSeconds;
  }

  public void verify(String signatureHeader, String rawBody) {
    long timestamp = -1;
    java.util.List<String> signatures = new java.util.ArrayList<>();
    if (signatureHeader != null) {
      for (String part : signatureHeader.split(",")) {
        String[] pair = part.trim().split("=", 2);
        if (pair.length == 2 && "t".equals(pair[0])) {
          timestamp = parseTimestamp(pair[1]);
        } else if (pair.length == 2 && "v1".equals(pair[0]) && !pair[1].isBlank()) {
          signatures.add(pair[1]);
        }
      }
    }
    long now = Instant.now().getEpochSecond();
    if (timestamp < 0
        || signatures.isEmpty()
        || timestamp < now - toleranceSeconds
        || timestamp > now + toleranceSeconds
        || !matches(timestamp, rawBody, signatures)) {
      throw unauthorized();
    }
  }

  private long parseTimestamp(String value) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException exception) {
      throw unauthorized();
    }
  }

  private boolean matches(long timestamp, String rawBody, java.util.List<String> signatures) {
    try {
      Mac mac = Mac.getInstance(HMAC_SHA_256);
      mac.init(new SecretKeySpec(secret, HMAC_SHA_256));
      byte[] expected = mac.doFinal((timestamp + "." + rawBody).getBytes(StandardCharsets.UTF_8));
      for (String signature : signatures) {
        try {
          byte[] supplied = HexFormat.of().parseHex(signature);
          if (MessageDigest.isEqual(expected, supplied)) {
            return true;
          }
        } catch (IllegalArgumentException ignored) {
          // Try any other v1 signature in the header to support secret rotation.
        }
      }
      return false;
    } catch (java.security.GeneralSecurityException exception) {
      throw new IllegalStateException("Could not verify Mux webhook signature", exception);
    }
  }

  private ResponseStatusException unauthorized() {
    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Mux webhook signature");
  }
}

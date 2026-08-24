package com.gii.api.service.auth;

import com.gii.api.exception.BadRequestApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.stereotype.Component;

@Component
public class GoogleCsrfTokenValidator {

  public void validate(String cookieToken, String requestToken) {
    if (cookieToken == null
        || cookieToken.isBlank()
        || requestToken == null
        || requestToken.isBlank()
        || !MessageDigest.isEqual(
            cookieToken.getBytes(StandardCharsets.UTF_8),
            requestToken.getBytes(StandardCharsets.UTF_8))) {
      throw new BadRequestApiException("Invalid Google CSRF token");
    }
  }
}

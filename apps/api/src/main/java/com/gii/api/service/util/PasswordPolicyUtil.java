package com.gii.api.service.util;

import com.gii.api.exception.UnprocessableEntityApiException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PasswordPolicyUtil {

  public static void validate(String password) {
    if (password == null || password.isBlank()) {
      throw new UnprocessableEntityApiException("Password is required");
    }
    if (password.length() < 8) {
      throw new UnprocessableEntityApiException("Password must be at least 8 characters");
    }
    boolean hasLetter = password.chars().anyMatch(Character::isLetter);
    boolean hasDigit = password.chars().anyMatch(Character::isDigit);
    if (!hasLetter || !hasDigit) {
      throw new UnprocessableEntityApiException(
          "Password must contain at least one letter and one digit");
    }
  }
}

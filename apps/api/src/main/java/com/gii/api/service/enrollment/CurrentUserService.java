package com.gii.api.service.enrollment;

import com.gii.common.entity.user.User;
import com.gii.common.repository.user.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

  private final UserRepository userRepository;

  public UUID getCurrentUserId(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated request");
    }

    Object principal = authentication.getPrincipal();

    if (principal instanceof User user) {
      return user.getId();
    }

    if (principal instanceof UUID userId) {
      return userId;
    }

    if (principal instanceof String value) {
      try {
        return UUID.fromString(value);
      } catch (IllegalArgumentException ignored) {
        throw new ResponseStatusException(
            HttpStatus.UNAUTHORIZED, "Invalid authentication principal");
      }
    }

    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication principal");
  }

  public User getCurrentUser(Authentication authentication) {
    UUID userId = getCurrentUserId(authentication);
    return userRepository
        .findById(userId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
  }
}

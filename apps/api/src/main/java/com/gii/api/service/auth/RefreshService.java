package com.gii.api.service.auth;

import com.gii.api.exception.ForbiddenApiException;
import com.gii.api.exception.UnauthorizedApiException;
import com.gii.api.model.response.auth.AuthResponse;
import com.gii.api.service.security.JwtService;
import com.gii.api.service.security.RefreshTokenCookieService;
import com.gii.api.service.security.RefreshTokenStoreService;
import com.gii.common.entity.user.User;
import com.gii.common.enums.UserStatus;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshService {

  private final JwtService jwtService;
  private final RefreshTokenStoreService refreshTokenStoreService;
  private final RefreshTokenCookieService refreshTokenCookieService;

  @Transactional(noRollbackFor = ForbiddenApiException.class)
  public AuthResponse execute(String oldRefreshToken, HttpServletResponse response) {
    if (oldRefreshToken == null || oldRefreshToken.isBlank()) {
      throw new UnauthorizedApiException("Invalid refresh token");
    }

    RefreshTokenStoreService.RefreshRotationResult rotation =
        refreshTokenStoreService.rotateRefreshToken(oldRefreshToken);
    User user = rotation.user();
    ensureActiveUser(user);

    // Generate new tokens
    String newAccessToken = jwtService.generateAccessToken(user);
    String newRefreshToken = rotation.refreshToken();

    refreshTokenCookieService.addRefreshTokenCookie(response, newRefreshToken);

    return AuthResponse.builder()
        .accessToken(newAccessToken)
        .isVerified(true)
        .userId(user.getId())
        .fullName(user.getFullName())
        .roles(user.getRoleNames())
        .build();
  }

  private void ensureActiveUser(User user) {
    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new UnauthorizedApiException("Invalid refresh token");
    }
  }
}

package com.gii.api.service.auth;

import static com.gii.api.service.util.PasswordPolicyUtil.validate;

import com.gii.api.exception.BadRequestApiException;
import com.gii.api.model.request.auth.ResetPasswordRequest;
import com.gii.api.model.response.auth.AuthResponse;
import com.gii.api.service.security.JwtService;
import com.gii.api.service.security.RefreshTokenCookieService;
import com.gii.api.service.security.RefreshTokenStoreService;
import com.gii.api.service.util.IdentifierNormalizationUtil;
import com.gii.common.entity.user.RefreshToken;
import com.gii.common.entity.user.User;
import com.gii.common.enums.UserStatus;
import com.gii.common.enums.VerificationChannel;
import com.gii.common.enums.VerificationPurpose;
import com.gii.common.repository.user.RefreshTokenRepository;
import com.gii.common.repository.user.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ResetPasswordService {

  private final VerificationCodeService verificationCodeService;
  private final PasswordEncoder passwordEncoder;
  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtService jwtService;
  private final RefreshTokenStoreService refreshTokenStoreService;
  private final RefreshTokenCookieService refreshTokenCookieService;

  public AuthResponse execute(ResetPasswordRequest request, HttpServletResponse response) {
    validate(request.newPassword());

    String normalizedIdentifier =
        IdentifierNormalizationUtil.normalizeIdentifier(request.channel(), request.identifier());
    Optional<User> userOpt = findUserByChannel(request.channel(), normalizedIdentifier);

    User user = userOpt.orElseThrow(() -> new BadRequestApiException("Invalid reset code"));

    verificationCodeService.verifyOtp(
        user.getId(), request.code(), VerificationPurpose.PASSWORD_RESET, request.channel());

    Instant now = Instant.now();

    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);

    List<RefreshToken> activeTokens =
        refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId());
    for (RefreshToken token : activeTokens) {
      token.setRevokedAt(now);
    }
    refreshTokenRepository.saveAll(activeTokens);

    ensureActiveUser(user);

    String accessToken = jwtService.generateAccessToken(user);
    String refreshToken = refreshTokenStoreService.createRefreshToken(user);
    refreshTokenCookieService.addRefreshTokenCookie(response, refreshToken);

    return AuthResponse.builder()
        .accessToken(accessToken)
        .isVerified(true)
        .userId(user.getId())
        .fullName(user.getFullName())
        .roles(user.getRoleNames())
        .build();
  }

  private void ensureActiveUser(User user) {
    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new BadRequestApiException("Invalid reset code");
    }
  }

  private Optional<User> findUserByChannel(
      VerificationChannel channel, String normalizedIdentifier) {
    return switch (channel) {
      case EMAIL -> userRepository.findByEmail(normalizedIdentifier);
      case PHONE -> userRepository.findByPhone(normalizedIdentifier);
    };
  }
}

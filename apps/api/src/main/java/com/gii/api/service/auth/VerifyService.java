package com.gii.api.service.auth;

import static com.gii.api.service.util.IdentifierNormalizationUtil.normalizeIdentifier;

import com.gii.api.exception.BadRequestApiException;
import com.gii.api.exception.UnauthorizedApiException;
import com.gii.api.model.request.auth.VerifyRequest;
import com.gii.api.model.response.auth.AuthResponse;
import com.gii.api.service.security.JwtService;
import com.gii.api.service.security.RefreshTokenCookieService;
import com.gii.api.service.security.RefreshTokenStoreService;
import com.gii.common.entity.user.User;
import com.gii.common.enums.UserStatus;
import com.gii.common.enums.VerificationChannel;
import com.gii.common.enums.VerificationPurpose;
import com.gii.common.repository.user.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class VerifyService {

  private final VerificationCodeService verificationCodeService;
  private final UserRepository userRepository;
  private final JwtService jwtService;
  private final RefreshTokenStoreService refreshTokenStoreService;
  private final RefreshTokenCookieService refreshTokenCookieService;

  public AuthResponse execute(VerifyRequest request, HttpServletResponse response) {
    if (request.purpose() == VerificationPurpose.PASSWORD_RESET) {
      throw new BadRequestApiException("Invalid verification purpose for this endpoint");
    }

    String normalizedIdentifier = normalizeIdentifier(request.channel(), request.identifier());
    Optional<User> userOpt = findUserByChannel(request.channel(), normalizedIdentifier);

    if (userOpt.isEmpty()) {
      throw new BadRequestApiException("Invalid verification code");
    }
    User user = userOpt.orElseThrow();

    verificationCodeService.verifyOtp(
        user.getId(), request.code(), request.purpose(), request.channel());

    Instant now = Instant.now();
    if (request.purpose() == VerificationPurpose.EMAIL_VERIFICATION
        && request.channel() == VerificationChannel.EMAIL) {
      user.setEmailVerifiedAt(now);
      userRepository.save(user);
    }
    if (request.purpose() == VerificationPurpose.PHONE_VERIFICATION
        && request.channel() == VerificationChannel.PHONE) {
      user.setPhoneVerifiedAt(now);
      userRepository.save(user);
    }

    ensureActiveUser(user);
    return createLoginResponse(user, response);
  }

  private Optional<User> findUserByChannel(
      VerificationChannel channel, String normalizedIdentifier) {
    return switch (channel) {
      case EMAIL -> userRepository.findByEmail(normalizedIdentifier);
      case PHONE -> userRepository.findByPhone(normalizedIdentifier);
    };
  }

  private void ensureActiveUser(User user) {
    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new UnauthorizedApiException("Invalid credentials");
    }
  }

  private AuthResponse createLoginResponse(User user, HttpServletResponse response) {
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
}

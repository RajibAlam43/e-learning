package com.gii.api.service.auth;

import com.gii.api.model.request.auth.ResetPasswordRequest;
import com.gii.api.service.util.IdentifierNormalizationUtil;
import com.gii.common.entity.user.RefreshToken;
import com.gii.common.entity.user.User;
import com.gii.common.enums.VerificationChannel;
import com.gii.common.enums.VerificationPurpose;
import com.gii.common.repository.user.RefreshTokenRepository;
import com.gii.common.repository.user.UserRepository;
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

  public void execute(ResetPasswordRequest request) {
    String normalizedIdentifier =
        IdentifierNormalizationUtil.normalizeIdentifier(request.channel(), request.identifier());
    Optional<User> userOpt = findUserByChannel(request.channel(), normalizedIdentifier);

    User user = userOpt.orElseThrow(() -> new RuntimeException("Invalid reset code"));

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
  }

  private Optional<User> findUserByChannel(
      VerificationChannel channel, String normalizedIdentifier) {
    return switch (channel) {
      case EMAIL -> userRepository.findByEmail(normalizedIdentifier);
      case PHONE -> userRepository.findByPhone(normalizedIdentifier);
    };
  }
}

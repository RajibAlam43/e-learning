package com.gii.api.service.auth;

import static com.gii.api.service.util.IdentifierNormalizationUtil.normalizeIdentifier;

import com.gii.api.model.request.auth.VerifyRequest;
import com.gii.common.entity.user.User;
import com.gii.common.enums.VerificationChannel;
import com.gii.common.enums.VerificationPurpose;
import com.gii.common.repository.user.UserRepository;
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

  public void execute(VerifyRequest request) {
    String normalizedIdentifier = normalizeIdentifier(request.channel(), request.identifier());
    Optional<User> userOpt = findUserByChannel(request.channel(), normalizedIdentifier);

    if (userOpt.isEmpty()) {
      throw new RuntimeException("Invalid verification code");
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
  }

  private Optional<User> findUserByChannel(
      VerificationChannel channel, String normalizedIdentifier) {
    return switch (channel) {
      case EMAIL -> userRepository.findByEmail(normalizedIdentifier);
      case PHONE -> userRepository.findByPhone(normalizedIdentifier);
    };
  }
}

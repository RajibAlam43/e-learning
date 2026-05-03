package com.gii.api.service.auth;

import com.gii.api.exception.BadRequestApiException;
import com.gii.api.exception.ForbiddenApiException;
import com.gii.api.exception.TooManyRequestsApiException;
import com.gii.api.service.util.EmailJobPublisherService;
import com.gii.api.service.util.IdentifierNormalizationUtil;
import com.gii.api.service.util.TokenHashUtil;
import com.gii.common.dto.EmailJobMessage;
import com.gii.common.entity.user.User;
import com.gii.common.entity.user.VerificationCode;
import com.gii.common.enums.EmailJobType;
import com.gii.common.enums.VerificationChannel;
import com.gii.common.enums.VerificationPurpose;
import com.gii.common.repository.user.UserRepository;
import com.gii.common.repository.user.VerificationCodeRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class VerificationCodeService {

  private final VerificationCodeRepository verificationCodeRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailJobPublisherService emailJobPublisherService;

  @Value("${otp.validity.minutes:15}")
  private int otpValidityMinutes;

  @Value("${otp.max-attempts:3}")
  private int maxAttempts;

  @Value("${otp.rate-limit.seconds:30}")
  private int rateLimitSeconds;

  private static final SecureRandom RANDOM = new SecureRandom();

  /** Generate and send OTP. Security: rate limiting + IP tracking + attempt limits. */
  public void generateAndSend(
      UUID userId, VerificationPurpose purpose, VerificationChannel channel, String channelValue) {
    final User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BadRequestApiException("User not found"));
    final Instant now = Instant.now();
    String normalizedChannelValue =
        IdentifierNormalizationUtil.normalizeIdentifier(channel, channelValue);
    String channelHash = TokenHashUtil.hash(normalizedChannelValue);

    // Per-user/channel cooldown
    Instant cooldownThreshold = now.minusSeconds(rateLimitSeconds);
    long userRecentCount =
        verificationCodeRepository.countRecentByUserAndChannel(userId, channel, cooldownThreshold);
    if (userRecentCount > 0) {
      throw new TooManyRequestsApiException("OTP already sent. Please try again later");
    }

    // Per-identifier cooldown (prevents same email/phone hammering)
    long identifierRecentCount =
        verificationCodeRepository.countRecentByChannelAndHash(
            channel, channelHash, cooldownThreshold);
    if (identifierRecentCount > 0) {
      throw new TooManyRequestsApiException("OTP already sent. Please try again later");
    }

    // Revoke previous valid OTP for same purpose+channel
    verificationCodeRepository
        .findLatestValidOtp(userId, purpose, channel)
        .ifPresent(
            existingOtp -> {
              existingOtp.setRevokedAt(now);
              verificationCodeRepository.save(existingOtp);
            });

    // Generate cryptographically secure 6-digit OTP
    String secureCode = generateSecureVerificationCode();
    String tokenHash = hashOtp(secureCode);

    String ipAddress = getClientIp();
    String userAgent = getUserAgent();

    VerificationCode verificationCode =
        VerificationCode.builder()
            .user(user)
            .purpose(purpose)
            .channel(channel)
            .tokenHash(tokenHash)
            .channelHash(channelHash)
            .expiresAt(now.plusSeconds(otpValidityMinutes * 60L))
            .attemptCount(0)
            .maxAttempts(maxAttempts)
            .sentCount(1)
            .lastSentAt(now)
            .requestedFromIp(ipAddress)
            .userAgent(userAgent)
            .securityMetadata(
                Map.of(
                    "created_from_ip", ipAddress,
                    "purpose", purpose.name(),
                    "channel", channel.name()))
            .build();

    verificationCodeRepository.save(verificationCode);

    queueOtpSend(userId, channel, normalizedChannelValue, secureCode, purpose);
  }

  /** Verify OTP with comprehensive security checks. */
  public void verifyOtp(
      UUID userId, String providedOtp, VerificationPurpose purpose, VerificationChannel channel) {
    final Instant now = Instant.now();
    String normalizedOtp = normalizeOtp(providedOtp);

    if (normalizedOtp.length() != 6 || !normalizedOtp.chars().allMatch(Character::isDigit)) {
      throw new BadRequestApiException("Invalid OTP");
    }

    VerificationCode verificationCode =
        verificationCodeRepository
            .findLatestValidOtp(userId, purpose, channel)
            .orElseThrow(() -> new BadRequestApiException("No valid OTP found"));

    // Security checks in order
    if (verificationCode.isRevoked()) {
      throw new ForbiddenApiException("OTP has been revoked");
    }

    if (verificationCode.isAlreadyUsed()) {
      throw new ForbiddenApiException("OTP already used");
    }

    if (verificationCode.isExpired()) {
      verificationCode.setRevokedAt(now);
      verificationCodeRepository.save(verificationCode);
      throw new BadRequestApiException("OTP expired");
    }

    if (verificationCode.isAttemptLimitReached()) {
      verificationCode.setRevokedAt(now);
      verificationCodeRepository.save(verificationCode);
      log.warn("Max OTP attempts reached for user {}", userId);
      throw new ForbiddenApiException("Maximum attempts exceeded. Request new OTP");
    }

    // Increment attempt counter
    verificationCode.setAttemptCount(verificationCode.getAttemptCount() + 1);

    // Constant-time comparison to prevent timing attacks
    boolean isValid = passwordEncoder.matches(normalizedOtp, verificationCode.getTokenHash());

    if (!isValid) {
      verificationCodeRepository.save(verificationCode);
      throw new BadRequestApiException("Invalid OTP");
    }

    // Mark as used
    verificationCode.setUsedAt(now);
    verificationCodeRepository.save(verificationCode);

    log.info("OTP verified successfully for user {} via {} for {}", userId, channel, purpose);
  }

  // ============ Security Helpers ============

  /** Generate cryptographically secure 6-digit verification code. */
  private String generateSecureVerificationCode() {
    int verificationCode = 100000 + RANDOM.nextInt(900000);
    return String.valueOf(verificationCode);
  }

  /** Hash verification code using password encoder (constant-time comparison). */
  private String hashOtp(String verificationCode) {
    return passwordEncoder.encode(verificationCode);
  }

  private String normalizeOtp(String otp) {
    return otp == null ? "" : otp.trim();
  }

  private String getClientIp() {
    ServletRequestAttributes attrs =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attrs == null) {
      return "UNKNOWN";
    }

    String forwardedFor = attrs.getRequest().getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    String realIp = attrs.getRequest().getHeader("X-Real-IP");
    if (realIp != null && !realIp.isBlank()) {
      return realIp.trim();
    }
    return attrs.getRequest().getRemoteAddr();
  }

  private String getUserAgent() {
    ServletRequestAttributes attrs =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attrs != null) {
      return attrs.getRequest().getHeader("User-Agent");
    }
    return "UNKNOWN";
  }

  private void queueOtpSend(
      UUID userId,
      VerificationChannel channel, String channelValue, String code, VerificationPurpose purpose) {
    if (channel != VerificationChannel.EMAIL) {
      log.info("Skipping email job publish for non-email channel {}", channel);
      return;
    }

    EmailJobMessage job =
        EmailJobMessage.builder()
            .userId(userId)
            .jobType(EmailJobType.OTP_VERIFICATION)
            .toEmail(channelValue)
            .subject("Your verification code")
            .body(
                "Your verification code is: %s. It will expire in %d minutes."
                    .formatted(code, otpValidityMinutes))
            .verificationPurpose(purpose)
            .verificationChannel(channel)
            .verificationCode(code)
            .createdAt(Instant.now())
            .build();

    emailJobPublisherService.publish(job);
  }
}

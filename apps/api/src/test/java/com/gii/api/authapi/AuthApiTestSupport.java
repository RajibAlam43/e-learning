package com.gii.api.authapi;

import com.gii.api.service.util.TokenHashUtil;
import com.gii.common.entity.user.RefreshToken;
import com.gii.common.entity.user.Role;
import com.gii.common.entity.user.User;
import com.gii.common.entity.user.UserRole;
import com.gii.common.entity.user.UserRoleId;
import com.gii.common.entity.user.VerificationCode;
import com.gii.common.enums.UserStatus;
import com.gii.common.enums.VerificationChannel;
import com.gii.common.enums.VerificationPurpose;
import com.gii.common.repository.user.RefreshTokenRepository;
import com.gii.common.repository.user.RoleRepository;
import com.gii.common.repository.user.UserRepository;
import com.gii.common.repository.user.UserRoleRepository;
import com.gii.common.repository.user.VerificationCodeRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

abstract class AuthApiTestSupport {

  @Autowired protected UserRepository userRepository;
  @Autowired protected RoleRepository roleRepository;
  @Autowired protected UserRoleRepository userRoleRepository;
  @Autowired protected VerificationCodeRepository verificationCodeRepository;
  @Autowired protected RefreshTokenRepository refreshTokenRepository;
  protected final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  protected void cleanAuthTables() {
    verificationCodeRepository.deleteAll();
    refreshTokenRepository.deleteAll();
    userRoleRepository.deleteAll();
    userRepository.deleteAll();
  }

  protected User user(
      String fullName, String email, String phone, String password, UserStatus status) {
    return userRepository.save(
        User.builder()
            .fullName(fullName)
            .email(email)
            .phone(phone)
            .phoneCountryCode(phone != null ? "+880" : null)
            .passwordHash(passwordEncoder.encode(password))
            .status(status)
            .build());
  }

  protected void addRole(User user, String roleName) {
    Role role =
        roleRepository
            .findByName(roleName)
            .orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));
    userRoleRepository.save(
        UserRole.builder()
            .user(user)
            .role(role)
            .id(UserRoleId.builder().userId(user.getId()).roleId(role.getId()).build())
            .build());
  }

  protected VerificationCode verificationCode(
      User user,
      VerificationPurpose purpose,
      VerificationChannel channel,
      String identifier,
      String otpCode,
      Instant expiresAt) {
    return verificationCodeRepository.save(
        VerificationCode.builder()
            .user(user)
            .purpose(purpose)
            .channel(channel)
            .channelHash(TokenHashUtil.hash(identifier))
            .tokenHash(passwordEncoder.encode(otpCode))
            .expiresAt(expiresAt)
            .attemptCount(0)
            .maxAttempts(3)
            .sentCount(1)
            .lastSentAt(Instant.now())
            .securityMetadata(Map.of("test", true))
            .build());
  }

  protected RefreshToken refreshToken(
      User user, String rawToken, UUID sessionId, Instant expiresAt) {
    return refreshTokenRepository.save(
        RefreshToken.builder()
            .user(user)
            .sessionId(sessionId)
            .tokenHash(TokenHashUtil.hash(rawToken))
            .expiresAt(expiresAt)
            .build());
  }
}

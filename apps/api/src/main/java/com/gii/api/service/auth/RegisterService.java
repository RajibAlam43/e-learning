package com.gii.api.service.auth;

import static com.gii.api.service.util.PasswordPolicyUtil.validate;

import com.gii.api.exception.BadRequestApiException;
import com.gii.api.exception.ConflictApiException;
import com.gii.api.model.request.auth.RegisterRequest;
import com.gii.api.model.response.auth.RegisterResponse;
import com.gii.common.entity.user.Role;
import com.gii.common.entity.user.User;
import com.gii.common.entity.user.UserRole;
import com.gii.common.entity.user.UserRoleId;
import com.gii.common.enums.VerificationChannel;
import com.gii.common.enums.VerificationPurpose;
import com.gii.common.repository.user.RoleRepository;
import com.gii.common.repository.user.UserRepository;
import com.gii.common.repository.user.UserRoleRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RegisterService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final VerificationCodeService verificationCodeService;
  private final RoleRepository roleRepository;
  private final UserRoleRepository userRoleRepository;

  public RegisterResponse execute(RegisterRequest request) {
    validate(request.password());

    String email = normalizeIdentifier(VerificationChannel.EMAIL, request.email());
    String phone = normalizeIdentifier(VerificationChannel.PHONE, request.phoneNumber());
    // Needs at least one of email or phone
    if (email == null && phone == null) {
      throw new BadRequestApiException("Either email or phone number must be provided");
    }

    // Don't allow both email and phone together
    if (email != null && phone != null) {
      throw new BadRequestApiException("Only one of email or phone number should be provided");
    }

    // Check email/phone uniqueness
    if (email != null && userRepository.existsByEmail(email)) {
      throw new ConflictApiException("Email already in use");
    }

    // Check email/phone uniqueness
    if (phone != null && userRepository.existsByPhone(phone)) {
      throw new ConflictApiException("Phone number already in use");
    }

    String countryCode = normalizeCountryCode(request.phoneCountryCode());
    if (phone != null && countryCode == null) {
      throw new BadRequestApiException("Country code is required when phone number is provided");
    }

    // Create user with NOT verified status
    User user =
        User.builder()
            .fullName(request.fullName())
            .email(email)
            .phone(phone)
            .phoneCountryCode(countryCode)
            .passwordHash(passwordEncoder.encode(request.password()))
            .build();

    userRepository.save(user);

    // Assign STUDENT role
    Role studentRole =
        roleRepository
            .findByName("STUDENT")
            .orElseThrow(() -> new BadRequestApiException("STUDENT role not found"));

    UserRole userRole =
        UserRole.builder()
            .user(user)
            .role(studentRole)
            .id(UserRoleId.builder().userId(user.getId()).roleId(studentRole.getId()).build())
            .build();

    userRoleRepository.save(userRole);

    // Generate and send OTP for email verification (if provided)
    if (email != null) {
      verificationCodeService.generateAndSend(
          user.getId(), VerificationPurpose.EMAIL_VERIFICATION, VerificationChannel.EMAIL, email);
    }

    // Generate and send OTP for phone verification (if provided)
    if (phone != null) {
      verificationCodeService.generateAndSend(
          user.getId(), VerificationPurpose.PHONE_VERIFICATION, VerificationChannel.PHONE, phone);
    }

    return RegisterResponse.builder()
        .userId(user.getId())
        .channel(email != null ? VerificationChannel.EMAIL : VerificationChannel.PHONE)
        .build();
  }

  private String normalizeIdentifier(VerificationChannel channel, String identifier) {
    if (identifier == null) {
      return null;
    }
    String value = identifier.trim();
    if (value.isEmpty()) {
      return null;
    }

    return switch (channel) {
      case EMAIL -> value.toLowerCase(Locale.ROOT);
      case PHONE -> value.replaceAll("[^0-9]", "");
    };
  }

  private String normalizeCountryCode(String countryCode) {
    if (countryCode == null) {
      return null;
    }
    String digits = countryCode.replaceAll("[^0-9]", "");
    return digits.isEmpty() ? null : "+" + digits;
  }
}

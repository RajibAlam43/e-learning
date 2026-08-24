package com.gii.api.service.auth;

import static com.gii.api.service.util.IdentifierNormalizationUtil.normalizeIdentifier;

import com.gii.api.exception.BadRequestApiException;
import com.gii.api.exception.UnauthorizedApiException;
import com.gii.api.model.request.auth.GoogleAuthRequest;
import com.gii.api.model.response.auth.AuthResponse;
import com.gii.api.service.auth.GoogleIdentityTokenVerifier.GoogleProfile;
import com.gii.api.service.security.JwtService;
import com.gii.api.service.security.RefreshTokenCookieService;
import com.gii.api.service.security.RefreshTokenStoreService;
import com.gii.common.entity.user.Role;
import com.gii.common.entity.user.User;
import com.gii.common.entity.user.UserRole;
import com.gii.common.entity.user.UserRoleId;
import com.gii.common.enums.UserStatus;
import com.gii.common.enums.VerificationChannel;
import com.gii.common.repository.user.RoleRepository;
import com.gii.common.repository.user.UserRepository;
import com.gii.common.repository.user.UserRoleRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class GoogleAuthService {

  private final GoogleIdentityTokenVerifier googleTokenVerifier;
  private final GoogleAuthEmailLockService googleAuthEmailLockService;
  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final UserRoleRepository userRoleRepository;
  private final JwtService jwtService;
  private final RefreshTokenStoreService refreshTokenStoreService;
  private final RefreshTokenCookieService refreshTokenCookieService;

  public AuthResponse execute(GoogleAuthRequest request, HttpServletResponse response) {
    GoogleProfile profile = googleTokenVerifier.verify(request.credential());
    String email = normalizeIdentifier(VerificationChannel.EMAIL, profile.email());
    requireGmail(email);
    googleAuthEmailLockService.lock(email);

    User user =
        userRepository
            .findByGoogleSubjectWithRoles(profile.subject())
            .orElseGet(() -> findOrCreateByEmail(profile, email));
    ensureActive(user);
    String linkedEmail = normalizeIdentifier(VerificationChannel.EMAIL, user.getEmail());
    if (!email.equals(linkedEmail)) {
      throw new UnauthorizedApiException("Google account email does not match linked user");
    }
    if (user.getGoogleSubject() == null) {
      user.setGoogleSubject(profile.subject());
    } else if (!user.getGoogleSubject().equals(profile.subject())) {
      throw new UnauthorizedApiException("Google account is already linked");
    }
    if (user.getEmailVerifiedAt() == null) {
      user.setEmailVerifiedAt(Instant.now());
    }
    user = userRepository.save(user);

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

  private User findOrCreateByEmail(GoogleProfile profile, String email) {
    return userRepository.findByEmailWithRoles(email).orElseGet(() -> createUser(profile, email));
  }

  private User createUser(GoogleProfile profile, String email) {
    User user =
        userRepository.save(
            User.builder()
                .email(email)
                .fullName(resolveName(profile.name(), email))
                .googleSubject(profile.subject())
                .emailVerifiedAt(Instant.now())
                .status(UserStatus.ACTIVE)
                .build());
    Role studentRole =
        roleRepository
            .findByName("STUDENT")
            .orElseThrow(() -> new BadRequestApiException("STUDENT role not found"));
    UserRole userRole =
        userRoleRepository.save(
            UserRole.builder()
                .user(user)
                .role(studentRole)
                .id(UserRoleId.builder().userId(user.getId()).roleId(studentRole.getId()).build())
                .build());
    user.getUserRoles().add(userRole);
    return user;
  }

  private String resolveName(String profileName, String email) {
    if (profileName != null && !profileName.isBlank()) {
      return profileName.trim();
    }
    int atIndex = email.indexOf('@');
    return atIndex > 0 ? email.substring(0, atIndex) : email;
  }

  private void requireGmail(String email) {
    if (!email.endsWith("@gmail.com")) {
      throw new UnauthorizedApiException("Google authentication requires a Gmail account");
    }
  }

  private void ensureActive(User user) {
    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new UnauthorizedApiException("Invalid credentials");
    }
  }
}

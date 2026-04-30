package com.gii.api.service.auth;

import com.gii.api.model.response.auth.AuthResponse;
import com.gii.api.model.request.auth.LoginRequest;
import com.gii.api.service.security.JwtService;
import com.gii.api.service.security.RefreshTokenCookieService;
import com.gii.api.service.security.RefreshTokenStoreService;
import com.gii.common.entity.user.User;
import com.gii.common.enums.VerificationChannel;
import com.gii.common.enums.VerificationPurpose;
import com.gii.common.repository.user.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenStoreService refreshTokenStoreService;
    private final RefreshTokenCookieService refreshTokenCookieService;
    private final VerificationCodeService verificationCodeService;


    public AuthResponse execute(LoginRequest request, HttpServletResponse response) {
        String normalizedIdentifier = normalizeIdentifier(request.channel(), request.identifier());

        User user;
        switch (request.channel()) {
            case EMAIL -> {
                user = userRepository.findByEmail(normalizedIdentifier)
                        .orElseThrow(() -> new RuntimeException("Invalid credentials"));
                verifyPassword(request, user.getPasswordHash());

                if (user.getEmail() == null) {
                    throw new RuntimeException("Invalid credentials");
                }

                if (user.getEmailVerifiedAt() != null) {
                    return handleVerifiedUserLogin(user, response);
                } else {
                    return handleUnverifiedUserLogin(user, request, VerificationPurpose.EMAIL_VERIFICATION, user.getEmail());
                }
            }
            case PHONE -> {
                user = userRepository.findByPhone(normalizedIdentifier)
                        .orElseThrow(() -> new RuntimeException("Invalid credentials"));
                verifyPassword(request, user.getPasswordHash());

                if (user.getPhone() == null) {
                    throw new RuntimeException("Invalid credentials");
                }

                if (user.getPhoneVerifiedAt() != null) {
                    return handleVerifiedUserLogin(user, response);
                } else {
                    return handleUnverifiedUserLogin(user, request, VerificationPurpose.PHONE_VERIFICATION, user.getPhone());
                }
            }
            default -> throw new RuntimeException("Invalid credentials");
        }
    }

    private String normalizeIdentifier(VerificationChannel channel, String identifier) {
        String value = identifier == null ? "" : identifier.trim();
        return switch (channel) {
            case EMAIL -> value.toLowerCase(Locale.ROOT);
            case PHONE -> value.replaceAll("[^0-9+]", "");
        };
    }

    private AuthResponse handleVerifiedUserLogin(User user, HttpServletResponse response) {
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

    private AuthResponse handleUnverifiedUserLogin(User user, LoginRequest request, VerificationPurpose purpose, String channelValue) {
        verificationCodeService.generateAndSend(
                user.getId(),
                purpose,
                request.channel(),
                channelValue
        );

        return AuthResponse.builder()
                .userId(user.getId())
                .isVerified(false)
                .channel(request.channel())
                .build();
    }

    private void verifyPassword(LoginRequest request, String passwordHash) {
        if (!passwordEncoder.matches(request.password(), passwordHash)) {
            throw new RuntimeException("Invalid credentials");
        }
    }
}

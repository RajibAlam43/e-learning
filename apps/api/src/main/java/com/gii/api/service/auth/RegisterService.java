package com.gii.api.service.auth;

import com.gii.api.model.response.AuthResponse;
import com.gii.api.model.request.RegisterRequest;
import com.gii.api.service.security.JwtService;
import com.gii.api.service.security.RefreshTokenCookieService;
import com.gii.api.service.security.RefreshTokenStoreService;
import com.gii.common.entity.user.User;
import com.gii.common.repository.user.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
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
    private final JwtService jwtService;
    private final RefreshTokenStoreService refreshTokenStoreService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    public AuthResponse execute(RegisterRequest request, HttpServletResponse response) {

        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email already in use");
        }

        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                //.emailVerified(false)
                //.role(UserRole.STUDENT)
                .build();

        userRepository.save(user);

        // 🔥 No AuthenticationManager needed here

        String accessToken = jwtService.generateAccessTokenFromUsername(user.getEmail());
        String refreshToken = refreshTokenStoreService.createRefreshToken(user);

        refreshTokenCookieService.addRefreshTokenCookie(response, refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .build();
    }
}
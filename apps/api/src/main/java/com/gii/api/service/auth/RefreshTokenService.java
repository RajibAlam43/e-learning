package com.gii.api.service.auth;

import com.gii.api.model.response.AuthResponse;
import com.gii.api.service.security.JwtService;
import com.gii.api.service.security.RefreshTokenCookieService;
import com.gii.api.service.security.RefreshTokenStoreService;
import com.gii.common.entity.user.User;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    private final JwtService jwtService;
    private final RefreshTokenStoreService refreshTokenStoreService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    public AuthResponse execute(String oldRefreshToken, HttpServletResponse response) {

        // validate and rotate
        User user = refreshTokenStoreService.validateAndRotate(oldRefreshToken);

        String newAccessToken = jwtService.generateAccessTokenFromUsername(user.getEmail());
        String newRefreshToken = refreshTokenStoreService.createRefreshToken(user);

        refreshTokenCookieService.addRefreshTokenCookie(response, newRefreshToken);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .build();
    }


}

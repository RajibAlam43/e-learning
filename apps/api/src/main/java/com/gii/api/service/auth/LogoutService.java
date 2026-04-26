package com.gii.api.service.auth;

import com.gii.api.service.security.RefreshTokenCookieService;
import com.gii.api.service.security.TokenHashService;
import com.gii.common.repository.user.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class LogoutService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHashService tokenHashService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    public void execute(String rawRefreshToken, HttpServletResponse response) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String tokenHash = tokenHashService.hash(rawRefreshToken);

            refreshTokenRepository.findByTokenHash(tokenHash)
                    .ifPresent(token -> {
                        token.setRevokedAt(Instant.now());
                        refreshTokenRepository.save(token);
                    });
        }

        refreshTokenCookieService.clearRefreshTokenCookie(response);
    }
}

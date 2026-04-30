package com.gii.api.service.auth;

import com.gii.api.service.security.RefreshTokenCookieService;
import com.gii.api.service.util.TokenHashUtil;
import com.gii.common.entity.user.RefreshToken;
import com.gii.common.repository.user.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class LogoutService {

  private final RefreshTokenRepository refreshTokenRepository;
  private final RefreshTokenCookieService refreshTokenCookieService;

  public void execute(String rawRefreshToken, HttpServletResponse response) {
    Instant now = Instant.now();
    if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
      String tokenHash = TokenHashUtil.hash(rawRefreshToken);

      refreshTokenRepository
          .findByTokenHash(tokenHash)
          .ifPresent(
              token -> {
                List<RefreshToken> sessionTokens =
                    refreshTokenRepository.findBySessionIdAndRevokedAtIsNull(token.getSessionId());
                sessionTokens.forEach(sessionToken -> sessionToken.setRevokedAt(now));
                refreshTokenRepository.saveAll(sessionTokens);
              });
    }

    refreshTokenCookieService.clearRefreshTokenCookie(response);
  }
}

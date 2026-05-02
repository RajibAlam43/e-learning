package com.gii.api.service.security;

import static com.gii.api.service.util.TokenHashUtil.hash;

import com.gii.api.exception.ForbiddenApiException;
import com.gii.api.exception.UnauthorizedApiException;
import com.gii.common.entity.user.RefreshToken;
import com.gii.common.entity.user.User;
import com.gii.common.repository.user.RefreshTokenRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenStoreService {

  private final RefreshTokenRepository repository;

  private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);
  private static final int REFRESH_TOKEN_BYTES = 32;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  public record RefreshRotationResult(User user, String refreshToken) {}

  @Transactional(noRollbackFor = ForbiddenApiException.class)
  public RefreshRotationResult rotateRefreshToken(String rawToken) {
    String hash = hash(rawToken);
    Instant now = Instant.now();

    RefreshToken token =
        repository
            .findByTokenHash(hash)
            .orElseThrow(() -> new UnauthorizedApiException("Invalid refresh token"));

    if (token.getRevokedAt() != null || token.getUsedAt() != null) {
      revokeActiveTokensBySession(token.getSessionId(), now);
      throw new ForbiddenApiException("Refresh token reuse detected");
    }

    if (token.getExpiresAt().isBefore(now)) {
      token.setRevokedAt(now);
      repository.save(token);
      throw new UnauthorizedApiException("Token expired");
    }

    String newRawToken = generateRawToken();
    String newHash = hash(newRawToken);

    RefreshToken replacement =
        RefreshToken.builder()
            .user(token.getUser())
            .tokenHash(newHash)
            .sessionId(token.getSessionId())
            .expiresAt(now.plus(REFRESH_TOKEN_TTL))
            .build();

    repository.save(replacement);

    token.setRevokedAt(now);
    token.setReplacedByToken(replacement);
    repository.save(token);

    return new RefreshRotationResult(token.getUser(), newRawToken);
  }

  public String createRefreshToken(User user) {
    String rawToken = generateRawToken();
    String hash = hash(rawToken);

    RefreshToken token =
        RefreshToken.builder()
            .user(user)
            .tokenHash(hash)
            .sessionId(UUID.randomUUID())
            .expiresAt(Instant.now().plus(REFRESH_TOKEN_TTL))
            .build();

    repository.save(token);

    return rawToken;
  }

  private String generateRawToken() {
    byte[] tokenBytes = new byte[REFRESH_TOKEN_BYTES];
    SECURE_RANDOM.nextBytes(tokenBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
  }

  private void revokeActiveTokensBySession(UUID sessionId, Instant now) {
    List<RefreshToken> sessionTokens = repository.findBySessionIdAndRevokedAtIsNull(sessionId);
    for (RefreshToken sessionToken : sessionTokens) {
      sessionToken.setRevokedAt(now);
    }
    repository.saveAll(sessionTokens);
  }
}

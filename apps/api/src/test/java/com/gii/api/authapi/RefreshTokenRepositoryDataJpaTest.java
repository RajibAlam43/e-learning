package com.gii.api.authapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.gii.common.entity.user.User;
import com.gii.common.enums.UserStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RefreshTokenRepositoryDataJpaTest extends AbstractAuthDataJpaTest {

  @AfterEach
  void cleanup() {
    cleanAuthTables();
  }

  @Test
  void sessionAndUserActiveTokenQueriesWork() {
    User user =
        user("Refresh Repo", "refresh-repo@example.com", null, "Secret123!", UserStatus.ACTIVE);
    UUID sessionId = UUID.randomUUID();
    refreshToken(user, "repo-token-1", sessionId, Instant.now().plusSeconds(1200));
    var revoked = refreshToken(user, "repo-token-2", sessionId, Instant.now().plusSeconds(1200));
    revoked.setRevokedAt(Instant.now());
    refreshTokenRepository.save(revoked);

    assertThat(refreshTokenRepository.findBySessionIdAndRevokedAtIsNull(sessionId)).hasSize(1);
    assertThat(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId())).hasSize(1);
  }
}

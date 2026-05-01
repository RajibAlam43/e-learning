package com.gii.common.repository.user;

import com.gii.common.entity.user.VerificationCode;
import com.gii.common.enums.VerificationChannel;
import com.gii.common.enums.VerificationPurpose;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, UUID> {

  // Find valid OTP (not used, not revoked, not expired, not limit reached)
  @Query(
      """
        SELECT o FROM VerificationCode o
        WHERE o.user.id = :userId
        AND o.purpose = :purpose
        AND o.channel = :channel
        AND o.usedAt IS NULL
        AND o.revokedAt IS NULL
        AND o.expiresAt > CURRENT_TIMESTAMP
        AND o.attemptCount < o.maxAttempts
        ORDER BY o.createdAt DESC
      """)
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<VerificationCode> findLatestValidOtp(
      UUID userId, VerificationPurpose purpose, VerificationChannel channel, Pageable pageable);

  default Optional<VerificationCode> findLatestValidOtp(
      UUID userId, VerificationPurpose purpose, VerificationChannel channel) {
    return findLatestValidOtp(userId, purpose, channel, Pageable.ofSize(1)).stream().findFirst();
  }

  @Query(
      """
        SELECT COUNT(o) FROM VerificationCode o
        WHERE o.user.id = :userId
        AND o.channel = :channel
        AND o.createdAt >= :threshold
      """)
  long countRecentByUserAndChannel(UUID userId, VerificationChannel channel, Instant threshold);

  @Query(
      """
        SELECT COUNT(o) FROM VerificationCode o
        WHERE o.channel = :channel
        AND o.channelHash = :channelHash
        AND o.createdAt >= :threshold
      """)
  long countRecentByChannelAndHash(
      VerificationChannel channel, String channelHash, Instant threshold);

  // Cleanup expired/revoked OTPs (run periodically)
  void deleteByExpiresAtBeforeAndUsedAtIsNotNull(Instant expiryTime);
}

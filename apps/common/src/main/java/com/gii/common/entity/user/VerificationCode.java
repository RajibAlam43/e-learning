package com.gii.common.entity.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.common.BaseTokenEntity;
import com.gii.common.enums.VerificationChannel;
import com.gii.common.enums.VerificationPurpose;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "verification_codes",
        indexes = {
                @Index(name = "idx_verification_codes_user", columnList = "user_id"),
                @Index(name = "idx_verification_codes_purpose", columnList = "purpose"),
                @Index(name = "idx_verification_codes_expires_at", columnList = "expires_at"),
                @Index(name = "idx_verification_codes_channel", columnList = "channel"),
                @Index(name = "idx_verification_codes_channel_hash", columnList = "channel_hash")
        }
)
public class VerificationCode extends BaseTokenEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 40)
    private VerificationPurpose purpose;

    // Channel: EMAIL or PHONE
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private VerificationChannel channel;

    // Hash of email/phone to prevent enumeration attacks
    @Column(name = "channel_hash", nullable = false, length = 255)
    private String channelHash;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private Integer maxAttempts = 3;

    @Column(name = "sent_count", nullable = false)
    @Builder.Default
    private Integer sentCount = 1;

    @Column(name = "last_sent_at", nullable = false)
    private Instant lastSentAt;

    // IP address that requested OTP (for security auditing)
    @Column(name = "requested_from_ip", length = 45)
    private String requestedFromIp;

    // Device fingerprint/user agent for additional verification
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "security_metadata", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> securityMetadata = Map.of();

    public boolean isAttemptLimitReached() {
        return attemptCount != null && maxAttempts != null && attemptCount >= maxAttempts;
    }
}

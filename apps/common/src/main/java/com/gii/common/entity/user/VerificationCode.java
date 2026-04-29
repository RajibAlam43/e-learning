package com.gii.common.entity.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.common.BaseTokenEntity;
import com.gii.common.enums.PhoneOtpPurpose;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

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
                @Index(name = "idx_verification_codes_expires_at", columnList = "expires_at")
        }
)
public class VerificationCode extends BaseTokenEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 40)
    private PhoneOtpPurpose purpose;

    // Hash of normalized E.164 phone used at send time
    @Column(name = "phone_hash", nullable = false, length = 255)
    private String phoneHash;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private Integer maxAttempts = 5;

    @Column(name = "sent_count", nullable = false)
    @Builder.Default
    private Integer sentCount = 1;

    @Column(name = "last_sent_at", nullable = false)
    private Instant lastSentAt;

    public boolean isAttemptLimitReached() {
        return attemptCount != null && maxAttempts != null && attemptCount >= maxAttempts;
    }
}

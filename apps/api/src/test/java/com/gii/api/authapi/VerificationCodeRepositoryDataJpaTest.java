package com.gii.api.authapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.gii.common.entity.user.User;
import com.gii.common.enums.UserStatus;
import com.gii.common.enums.VerificationChannel;
import com.gii.common.enums.VerificationPurpose;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class VerificationCodeRepositoryDataJpaTest extends AbstractAuthDataJpaTest {

  @AfterEach
  void cleanup() {
    cleanAuthTables();
  }

  @Test
  void latestValidOtpAndRecentCountersWork() {
    User user = user("Otp User", "otp-repo@example.com", null, "Secret123!", UserStatus.ACTIVE);
    verificationCode(
        user,
        VerificationPurpose.EMAIL_VERIFICATION,
        VerificationChannel.EMAIL,
        "otp-repo@example.com",
        "111111",
        Instant.now().plusSeconds(1200));

    var latest =
        verificationCodeRepository.findLatestValidOtp(
            user.getId(), VerificationPurpose.EMAIL_VERIFICATION, VerificationChannel.EMAIL);
    assertThat(latest).isPresent();

    Instant threshold = Instant.now().minusSeconds(60);
    assertThat(
            verificationCodeRepository.countRecentByUserAndChannel(
                user.getId(), VerificationChannel.EMAIL, threshold))
        .isEqualTo(1);
    assertThat(
            verificationCodeRepository.countRecentByChannelAndHash(
                VerificationChannel.EMAIL,
                com.gii.api.service.util.TokenHashUtil.hash("otp-repo@example.com"),
                threshold))
        .isEqualTo(1);
  }
}

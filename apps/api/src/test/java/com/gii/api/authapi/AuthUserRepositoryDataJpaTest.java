package com.gii.api.authapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.gii.common.entity.user.User;
import com.gii.common.enums.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AuthUserRepositoryDataJpaTest extends AbstractAuthDataJpaTest {

  @AfterEach
  void cleanup() {
    cleanAuthTables();
  }

  @Test
  void findByEmailAndPhoneAndExistsChecksWork() {
    User user =
        user("Repo User", "repo@example.com", "01712345678", "Secret123!", UserStatus.ACTIVE);

    assertThat(userRepository.findByEmail("repo@example.com")).isPresent();
    assertThat(userRepository.findByPhone("01712345678")).isPresent();
    assertThat(userRepository.existsByEmail("repo@example.com")).isTrue();
    assertThat(userRepository.existsByPhone("01712345678")).isTrue();
    assertThat(userRepository.findById(user.getId())).isPresent();
  }
}

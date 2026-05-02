package com.gii.api.meapi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MeProfileRepositoriesDataJpaTest extends AbstractMeDataJpaTest {

  @AfterEach
  void cleanup() {
    cleanupMeData();
  }

  @Test
  void userAndProfileRepositoriesSupportGetAndUpdatePaths() {
    var u1 = user("User1", "u1@example.com", "+8801710000101");
    var u2 = user("User2", "u2@example.com", "+8801710000102");
    profile(u1, "en-US", "Asia/Dhaka", "https://cdn.test/u1.png", "bio1");
    instructorProfile(u1, "Teacher U1");

    assertThat(userRepository.findByEmail("u1@example.com")).isPresent();
    assertThat(userRepository.findByPhone("+8801710000101")).isPresent();
    assertThat(userRepository.existsByEmail("u2@example.com")).isTrue();
    assertThat(userRepository.existsByPhone("+8801710000102")).isTrue();
    assertThat(userProfileRepository.findById(u1.getId())).isPresent();
    assertThat(instructorProfileRepository.findById(u1.getId())).isPresent();
  }
}

package com.gii.api.quizapi;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataJpaTest
@ActiveProfiles("local")
abstract class AbstractQuizDataJpaTest extends QuizApiTestSupport {

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", SharedQuizPostgresContainer.INSTANCE::getJdbcUrl);
    registry.add("spring.datasource.username", SharedQuizPostgresContainer.INSTANCE::getUsername);
    registry.add("spring.datasource.password", SharedQuizPostgresContainer.INSTANCE::getPassword);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    registry.add("spring.flyway.enabled", () -> "true");
  }
}

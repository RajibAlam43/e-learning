package com.gii.api.lessonapi;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataJpaTest
@ActiveProfiles("local")
abstract class AbstractLessonDataJpaTest extends LessonApiTestSupport {

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", SharedLessonPostgresContainer.INSTANCE::getJdbcUrl);
    registry.add("spring.datasource.username", SharedLessonPostgresContainer.INSTANCE::getUsername);
    registry.add("spring.datasource.password", SharedLessonPostgresContainer.INSTANCE::getPassword);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    registry.add("spring.flyway.enabled", () -> "true");
  }
}

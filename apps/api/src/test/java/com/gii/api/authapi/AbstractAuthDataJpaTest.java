package com.gii.api.authapi;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataJpaTest
@ActiveProfiles("local")
abstract class AbstractAuthDataJpaTest extends AuthApiTestSupport {

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", SharedAuthPostgresContainer.INSTANCE::getJdbcUrl);
    registry.add("spring.datasource.username", SharedAuthPostgresContainer.INSTANCE::getUsername);
    registry.add("spring.datasource.password", SharedAuthPostgresContainer.INSTANCE::getPassword);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    registry.add("spring.flyway.enabled", () -> "true");
  }
}

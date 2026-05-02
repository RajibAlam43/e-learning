package com.gii.api.adminapi;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataJpaTest
@ActiveProfiles("local")
abstract class AbstractAdminDataJpaTest extends AdminApiTestSupport {

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", SharedAdminPostgresContainer.INSTANCE::getJdbcUrl);
    registry.add("spring.datasource.username", SharedAdminPostgresContainer.INSTANCE::getUsername);
    registry.add("spring.datasource.password", SharedAdminPostgresContainer.INSTANCE::getPassword);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    registry.add("spring.flyway.enabled", () -> "true");
  }
}

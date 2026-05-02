package com.gii.api.studentapi;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
abstract class AbstractStudentApiIntegrationTest extends StudentApiTestSupport {

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", SharedStudentPostgresContainer.INSTANCE::getJdbcUrl);
    registry.add(
        "spring.datasource.username", SharedStudentPostgresContainer.INSTANCE::getUsername);
    registry.add(
        "spring.datasource.password", SharedStudentPostgresContainer.INSTANCE::getPassword);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    registry.add("spring.flyway.enabled", () -> "true");
    registry.add("app.jwt.secret", () -> "dGVzdF9zZWNyZXRfdGVzdF9zZWNyZXRfdGVzdF9zZWNyZXRfMTIz");
    registry.add("app.jwt.access-token-expiration-ms", () -> "900000");
    registry.add("spring.data.redis.host", () -> "localhost");
    registry.add("spring.data.redis.port", () -> "6379");
  }
}

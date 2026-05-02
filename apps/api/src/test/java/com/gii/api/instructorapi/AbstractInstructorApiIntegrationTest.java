package com.gii.api.instructorapi;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
abstract class AbstractInstructorApiIntegrationTest extends InstructorApiTestSupport {

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", SharedInstructorPostgresContainer.INSTANCE::getJdbcUrl);
    registry.add(
        "spring.datasource.username", SharedInstructorPostgresContainer.INSTANCE::getUsername);
    registry.add(
        "spring.datasource.password", SharedInstructorPostgresContainer.INSTANCE::getPassword);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    registry.add("spring.flyway.enabled", () -> "true");
    registry.add("app.jwt.secret", () -> "dGVzdF9zZWNyZXRfdGVzdF9zZWNyZXRfdGVzdF9zZWNyZXRfMTIz");
    registry.add("app.jwt.access-token-expiration-ms", () -> "900000");
    registry.add("bunny.token-security-key", () -> "test-bunny-key");
    registry.add("mux.signing-key-id", () -> "test-signing-key");
  }
}

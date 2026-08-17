package com.gii.api.meapi;

import com.gii.api.testsupport.SharedPostgresContainer;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
abstract class AbstractMeApiIntegrationTest extends MeApiTestSupport {

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", SharedPostgresContainer.INSTANCE::getJdbcUrl);
    registry.add("spring.datasource.username", SharedPostgresContainer.INSTANCE::getUsername);
    registry.add("spring.datasource.password", SharedPostgresContainer.INSTANCE::getPassword);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    registry.add("spring.flyway.enabled", () -> "true");
    registry.add("app.jwt.secret", () -> "dGVzdF9zZWNyZXRfdGVzdF9zZWNyZXRfdGVzdF9zZWNyZXRfMTIz");
    registry.add("app.jwt.access-token-expiration-ms", () -> "900000");
  }
}

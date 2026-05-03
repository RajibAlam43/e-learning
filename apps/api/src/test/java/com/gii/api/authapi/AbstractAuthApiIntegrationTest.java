package com.gii.api.authapi;

import com.gii.api.service.util.EmailJobPublisherService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
abstract class AbstractAuthApiIntegrationTest extends AuthApiTestSupport {

  @MockitoBean protected EmailJobPublisherService emailJobPublisherService;

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", SharedAuthPostgresContainer.INSTANCE::getJdbcUrl);
    registry.add("spring.datasource.username", SharedAuthPostgresContainer.INSTANCE::getUsername);
    registry.add("spring.datasource.password", SharedAuthPostgresContainer.INSTANCE::getPassword);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    registry.add("spring.flyway.enabled", () -> "true");
    registry.add("app.jwt.secret", () -> "dGVzdF9zZWNyZXRfdGVzdF9zZWNyZXRfdGVzdF9zZWNyZXRfMTIz");
    registry.add("app.jwt.access-token-expiration-ms", () -> "900000");
    registry.add("spring.data.redis.host", () -> "localhost");
    registry.add("spring.data.redis.port", () -> "6379");
  }
}

package com.gii.api.paymentapi;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
abstract class AbstractPaymentApiIntegrationTest extends PaymentApiTestSupport {

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", SharedPaymentPostgresContainer.INSTANCE::getJdbcUrl);
    registry.add(
        "spring.datasource.username", SharedPaymentPostgresContainer.INSTANCE::getUsername);
    registry.add(
        "spring.datasource.password", SharedPaymentPostgresContainer.INSTANCE::getPassword);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    registry.add("spring.flyway.enabled", () -> "true");
    registry.add("app.jwt.secret", () -> "dGVzdF9zZWNyZXRfdGVzdF9zZWNyZXRfdGVzdF9zZWNyZXRfMTIz");
    registry.add("app.jwt.access-token-expiration-ms", () -> "900000");
    registry.add("payments.sslcommerz.webhook-secret", () -> "ssl-test-secret");
    registry.add("payments.bkash.webhook-secret", () -> "bkash-test-secret");
  }
}

package com.gii.api.paymentapi;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataJpaTest
@ActiveProfiles("local")
abstract class AbstractPaymentDataJpaTest extends PaymentApiTestSupport {

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", SharedPaymentPostgresContainer.INSTANCE::getJdbcUrl);
    registry.add(
        "spring.datasource.username", SharedPaymentPostgresContainer.INSTANCE::getUsername);
    registry.add(
        "spring.datasource.password", SharedPaymentPostgresContainer.INSTANCE::getPassword);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    registry.add("spring.flyway.enabled", () -> "true");
  }
}

package com.gii.api.paymentapi;

import com.gii.api.testsupport.SharedPostgresContainer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.gii.api.service.payment.bkash.BkashSnsSignatureVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
abstract class AbstractPaymentApiIntegrationTest extends PaymentApiTestSupport {

  @MockitoBean protected BkashSnsSignatureVerifier bkashSnsSignatureVerifier;

  @BeforeEach
  void setUpBkashVerifierMock() {
    when(bkashSnsSignatureVerifier.isValid(any())).thenReturn(true);
  }

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", SharedPostgresContainer.INSTANCE::getJdbcUrl);
    registry.add(
        "spring.datasource.username", SharedPostgresContainer.INSTANCE::getUsername);
    registry.add(
        "spring.datasource.password", SharedPostgresContainer.INSTANCE::getPassword);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    registry.add("spring.flyway.enabled", () -> "true");
    registry.add("app.jwt.secret", () -> "dGVzdF9zZWNyZXRfdGVzdF9zZWNyZXRfdGVzdF9zZWNyZXRfMTIz");
    registry.add("app.jwt.access-token-expiration-ms", () -> "900000");
    registry.add("mux.signing-key-id", () -> "test-mux-signing-key");
    registry.add("mux.private-key-pem", () -> "test-mux-private-key");
    registry.add("bunny.token-security-key", () -> "test-bunny-key");
    registry.add("storage.r2.account-id", () -> "test-r2-account");
    registry.add("storage.r2.access-key-id", () -> "test-r2-access");
    registry.add("storage.r2.secret-access-key", () -> "test-r2-secret");
    registry.add("storage.r2.bucket", () -> "test-r2-bucket");
    registry.add("payments.callback-base-url", () -> "");
    registry.add("payments.sslcommerz.session-api-url", () -> "");
    registry.add(
        "payments.sslcommerz.validation-api-url",
        () -> "http://127.0.0.1:9/validator/api/validationserverAPI.php");
    registry.add("payments.sslcommerz.store-id", () -> "test-store");
    registry.add("payments.sslcommerz.store-password", () -> "test-password");
    registry.add("payments.sslcommerz.validate-on-webhook", () -> "false");
    registry.add("payments.bkash.username", () -> "test-user");
    registry.add("payments.bkash.password", () -> "test-pass");
    registry.add("payments.bkash.app-key", () -> "test-app-key");
    registry.add("payments.bkash.app-secret", () -> "test-app-secret");
    registry.add("redirect.subdomain", () -> "stage-api");
    registry.add("payments.sslcommerz.webhook-secret", () -> "ssl-test-secret");
    registry.add("payments.bkash.webhook-secret", () -> "bkash-test-secret");
  }
}

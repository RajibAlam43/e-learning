package com.gii.api.adminapi;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
abstract class AbstractAdminApiIntegrationTest extends AdminApiTestSupport {

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", SharedAdminPostgresContainer.INSTANCE::getJdbcUrl);
    registry.add("spring.datasource.username", SharedAdminPostgresContainer.INSTANCE::getUsername);
    registry.add("spring.datasource.password", SharedAdminPostgresContainer.INSTANCE::getPassword);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    registry.add("spring.flyway.enabled", () -> "true");
    registry.add("app.jwt.secret", () -> "dGVzdF9zZWNyZXRfdGVzdF9zZWNyZXRfdGVzdF9zZWNyZXRfMTIz");
    registry.add("app.jwt.access-token-expiration-ms", () -> "900000");
    registry.add("bunny.token-security-key", () -> "test-bunny-key");
    registry.add("mux.signing-key-id", () -> "test-signing-key");
    registry.add(
        "mux.private-key-prem",
        () ->
            "-----BEGIN PRIVATE KEY-----\n"
                + "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCe/6Z+X4rnW+2I\n"
                + "3WoR9X1gbK8469qvw00iA8CPNB2zwJOgQZj+QvkQKLWe1ABnDf1Ye9GmFX+2YzMp\n"
                + "jZfRTNnSXNqnSuSrwc3QZ5cAKzqHm4q7KMu0aeD6mSryhmNHGlG7O3yFyHDMvU5G\n"
                + "4i5rzocPcU9cTI0EEAPNin/xs3eP1ee3XbFWFRYoK8LVTPA/PJsCAYEFC8rnaYnU\n"
                + "worY/6mfiC4+GYX8iF/sjqBi5BQDW2asTWzfsOV04TlkUP4hpjiTe5tl8bSPViGZ\n"
                + "EpWku/aWuXXJDO0ylBEdzOtEVyYUKHbSNmSP0GNwuUL0NoQUL0Hju3dTDK3uMeUp\n"
                + "aqF9ELR1AgMBAAECggEAD8HoXzfazUkIBntjCrqYnVjIGM0PFEI3v7o9eDPDCdB5\n"
                + "EPXPUsuSYW6XaVE7ZPs283D5+wrAaHP2WxTkSeLi5306UkAS347Kde/+QA8Isu3Q\n"
                + "VvIZFh00JcHORtDYv/A0z2h608tkmnXcbxv1W8vRCM/C/LO9efyYU+N5AJw3qjJx\n"
                + "0EAjEBqgGJ/Dc12f78Go3080fcUiLyy+rCmC73ylag7HU6R0yziD/Y+bBaS2N4YJ\n"
                + "+0ajN/ozx/2TCw7E5WCaR6dX+xWQv9zJPyXYuQJSZ2+y0e1C6KzequXx2tb6PyB6\n"
                + "tZWDoxqcOg1leztT5ep9cHgJRkpEKlF3/awWEDMdNQKBgQDNqmKiDgORD8QkueNX\n"
                + "QSsyS4zF0zqYjF6Ex8DEVX0qtJRyhuqr3lgZCbFf269Hv5QjamGkRogJoQ4uX4a1\n"
                + "Uwv3TEXSwfNoMkbzTWj6Js74pkfMoPWkk2wOIxHGdSQfPJcMcPtJ3Fu0ydZLKNHD\n"
                + "yLj992t56gpQv6UHzNsfUmgo5wKBgQDF6W4tqTL5rMSiR6woVFz7apRFYmQjKzI0\n"
                + "Rgh43clUC8bxV7qFd6X2KVCOo4RDkIJYc0iWFWD+vdEcQe4dSYShNDoYKb8BJJiT\n"
                + "tnkr6fXYEi3bYtDlJBJ6QhZcE93jFq6g417EHxaV87idgnGsk+Qt+HkSAuc6g1ig\n"
                + "skj3VgIAQwKBgAfnr6jY7wwpVfmvZ2yYJafmPX/xEYSBiLSD1QX23zu/+yC3zNdU\n"
                + "UYCM3dN4ZzFTzMbbtOsShvIPzbK8mznm+kPEG77xE4ECxbVeWVWcHkJyaboUybxM\n"
                + "qk+Uy38cS4oj0w54XUBGhwPY6jKzW/Sxh2LDms7xzvmxlA9LsXRvv28rAoGAelJu\n"
                + "kLY1FXAdEJfv5fVOiu4yuje34KzAGOL4NYwO9W1aBqzMUdXDs8ORULfr9b6JcZW/\n"
                + "VVPdYVV8u1Rckalw2hnnliunLFOsIg/0EJpIIsYJpcELj7Sd3wadv0dgKw1H3ZTq\n"
                + "a5kzYncCsgd/rsSxkWAzpFg7BkVML/ZBdu2nyvUCgYEAnPB/8Y2NAK/aSUSmhxbZ\n"
                + "7yG+z7abGoV0iziJs4QPkctykzIMNtrXtQ/qdrLx/dRDxlWUpCvC4ahE/7sRr7ub\n"
                + "31/JwwXiQYqyt3J6BMoaxVIX6mW+g+fp2yE7tVZULEGuxgL138/3ZIC2dPM96DW1\n"
                + "n4X1covoxMcYBOFDKZImYu8=\n"
                + "-----END PRIVATE KEY-----");
  }
}

package com.gii.api.instructorapi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.gii.api.service.live.LiveMeetingCreateResult;
import com.gii.api.service.live.LiveMeetingProvisioningService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
abstract class AbstractInstructorApiIntegrationTest extends InstructorApiTestSupport {
  @MockitoBean protected LiveMeetingProvisioningService liveMeetingProvisioningService;

  @BeforeEach
  void setupMeetingProvisioningMock() {
    when(liveMeetingProvisioningService.createMeeting(any()))
        .thenReturn(
            LiveMeetingCreateResult.builder()
                .meetingId("m-" + UUID.randomUUID())
                .hostStartUrl("https://host.test/start/" + UUID.randomUUID())
                .participantJoinUrl("https://host.test/join/" + UUID.randomUUID())
                .build());
  }

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

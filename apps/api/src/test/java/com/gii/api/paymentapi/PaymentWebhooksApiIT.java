package com.gii.api.paymentapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.OrderStatus;
import com.gii.common.enums.PaymentEventStatus;
import com.gii.common.enums.PublishStatus;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class PaymentWebhooksApiIt extends AbstractPaymentApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupPaymentData();
  }

  @Test
  void sslcommerzWebhookWithValidSignatureShouldAcknowledgeAndPersistEvent() throws Exception {
    var student = user("Student Hook", "student-payment-hook@example.com");
    var creator = user("Creator Hook", "creator-payment-hook@example.com");
    var course =
        course(
            "Webhook Course",
            "webhook-course-payment",
            creator,
            PublishStatus.PUBLISHED,
            BigDecimal.valueOf(1000));
    var order =
        order(
            student,
            OrderStatus.PENDING,
            OrderProvider.SSLCOMMERZ,
            "txn-ssl-hook",
            BigDecimal.valueOf(1000));
    orderItem(order, course, BigDecimal.valueOf(1000), BigDecimal.ZERO);

    String payload = "{\"event\":\"payment\",\"txn\":\"txn-ssl-hook\"}";
    String signature = hmacBase64(payload, "ssl-test-secret");

    mockMvc
        .perform(
            post("/public/webhooks/payments/sslcommerz")
                .with(authentication(adminAuth(student.getId())))
                .contentType(MediaType.TEXT_PLAIN)
                .header("x-signature", signature)
                .header("x-transaction-id", "txn-ssl-hook")
                .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.acknowledged").value(true));

    assertThat(paymentEventRepository.findByOrderId(order.getId()))
        .anyMatch(
            event ->
                event.getProvider() == OrderProvider.SSLCOMMERZ
                    && event.getStatus() == PaymentEventStatus.RECEIVED);
  }

  @Test
  void bkashWebhookWithInvalidSignatureShouldBeBadRequest() throws Exception {
    String payload = "{\"event\":\"payment\"}";
    mockMvc
        .perform(
            post("/public/webhooks/payments/bkash")
                .with(
                    authentication(adminAuth(user("Admin Hook", "admin-hook@example.com").getId())))
                .contentType(MediaType.TEXT_PLAIN)
                .header("x-signature", "invalid")
                .content(payload))
        .andExpect(status().isBadRequest());
  }

  @Test
  void nagadWebhookShouldAcknowledgeAsNotEnabled() throws Exception {
    mockMvc
        .perform(
            post("/public/webhooks/payments/nagad")
                .with(authentication(adminAuth(user("Admin N", "admin-nagad@example.com").getId())))
                .contentType(MediaType.TEXT_PLAIN)
                .content("{}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.acknowledged").value(false));
  }

  @Test
  void sslcommerzWebhookShouldAcceptHexSignatureFormat() throws Exception {
    String payload = "{\"event\":\"payment\",\"txn\":\"txn-hex\"}";
    String hexSig = hmacHex(payload, "ssl-test-secret");

    mockMvc
        .perform(
            post("/public/webhooks/payments/sslcommerz")
                .contentType(MediaType.TEXT_PLAIN)
                .header("x-signature", "sha256=" + hexSig)
                .header("x-event-id", "evt-hex-1")
                .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.acknowledged").value(true));
  }

  @Test
  void sslcommerzWebhookMissingSignatureShouldBeBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/public/webhooks/payments/sslcommerz")
                .contentType(MediaType.TEXT_PLAIN)
                .content("{\"event\":\"payment\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void sslcommerzWebhookReplayShouldBeIdempotent() throws Exception {
    String payload = "{\"event\":\"payment\",\"txn\":\"txn-replay\"}";
    String signature = hmacBase64(payload, "ssl-test-secret");

    MvcResult first =
        mockMvc
            .perform(
                post("/public/webhooks/payments/sslcommerz")
                    .contentType(MediaType.TEXT_PLAIN)
                    .header("x-signature", signature)
                    .header("x-event-id", "evt-replay-1")
                    .content(payload))
            .andExpect(status().isOk())
            .andReturn();

    String firstBody = first.getResponse().getContentAsString();

    MvcResult second =
        mockMvc
            .perform(
                post("/public/webhooks/payments/sslcommerz")
                    .contentType(MediaType.TEXT_PLAIN)
                    .header("x-signature", signature)
                    .header("x-event-id", "evt-replay-1")
                    .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.acknowledged").value(true))
            .andReturn();

    assertThat(
            paymentEventRepository.findByProviderAndProviderEventId(
                OrderProvider.SSLCOMMERZ, "evt-replay-1"))
        .isPresent();
    assertThat(
            paymentEventRepository.findAll().stream()
                .filter(e -> "evt-replay-1".equals(e.getProviderEventId()))
                .count())
        .isEqualTo(1);
    assertThat(second.getResponse().getContentAsString()).contains("Webhook");
    assertThat(firstBody).isNotBlank();
  }

  private String hmacBase64(String payload, String secret) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return java.util.Base64.getEncoder()
          .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private String hmacHex(String payload, String secret) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(digest.length * 2);
      for (byte b : digest) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}

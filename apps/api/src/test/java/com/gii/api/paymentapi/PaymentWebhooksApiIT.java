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
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
  void sslcommerzWebhookWithFailedStatusShouldAcknowledgeAndPersistEvent() throws Exception {
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

    String payload = signedSslPayload("tran_id=txn-ssl-hook&status=FAILED&val_id=val-1");

    mockMvc
        .perform(
            post("/public/webhooks/payments/sslcommerz")
                .with(authentication(adminAuth(student.getId())))
                .contentType(MediaType.TEXT_PLAIN)
                .header("x-event-id", "evt-ssl-hook")
                .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.acknowledged").value(true));

    assertThat(paymentEventRepository.findByOrderId(order.getId()))
        .anyMatch(
            event ->
                event.getProvider() == OrderProvider.SSLCOMMERZ
                    && event.getStatus() == PaymentEventStatus.PROCESSED);
  }

  @Test
  void sslcommerzWebhookWithMixedCaseHeadersShouldProcessCancelledTransition() throws Exception {
    var student = user("Student Mixed", "student-payment-mixed@example.com");
    var creator = user("Creator Mixed", "creator-payment-mixed@example.com");
    var course =
        course(
            "Webhook Mixed",
            "webhook-mixed-payment",
            creator,
            PublishStatus.PUBLISHED,
            BigDecimal.valueOf(1000));
    var order =
        order(
            student,
            OrderStatus.PENDING,
            OrderProvider.SSLCOMMERZ,
            "txn-mixed-hook",
            BigDecimal.valueOf(1000));
    orderItem(order, course, BigDecimal.valueOf(1000), BigDecimal.ZERO);

    String payload = signedSslPayload("tran_id=txn-mixed-hook&status=CANCELLED&val_id=val-2");

    mockMvc
        .perform(
            post("/public/webhooks/payments/sslcommerz")
                .contentType(MediaType.TEXT_PLAIN)
                .header("X-Event-Id", "evt-mixed-1")
                .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.acknowledged").value(true));

    assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
        .isEqualTo(OrderStatus.CANCELLED);
    assertThat(
            enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                student.getId(), course.getId(), com.gii.common.enums.EnrollmentStatus.ACTIVE))
        .isFalse();
  }

  @Test
  void sslcommerzWebhookUnknownPaymentPayloadShouldNotMarkPaid() throws Exception {
    var student = user("Student Unknown", "student-payment-unknown@example.com");
    var creator = user("Creator Unknown", "creator-payment-unknown@example.com");
    var course =
        course(
            "Webhook Unknown",
            "webhook-unknown-payment",
            creator,
            PublishStatus.PUBLISHED,
            BigDecimal.valueOf(1000));
    var order =
        order(
            student,
            OrderStatus.PENDING,
            OrderProvider.SSLCOMMERZ,
            "txn-unknown-hook",
            BigDecimal.valueOf(1000));
    orderItem(order, course, BigDecimal.valueOf(1000), BigDecimal.ZERO);

    String payload = signedSslPayload("tran_id=txn-unknown-hook&status=PROCESSING&val_id=val-unknown");

    mockMvc
        .perform(
            post("/public/webhooks/payments/sslcommerz")
                .contentType(MediaType.TEXT_PLAIN)
                .header("x-transaction-id", "txn-unknown-hook")
                .header("x-event-id", "evt-unknown-1")
                .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.acknowledged").value(true));

    assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
        .isEqualTo(OrderStatus.PENDING);
    assertThat(
            enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                student.getId(), course.getId(), com.gii.common.enums.EnrollmentStatus.ACTIVE))
        .isFalse();
    assertThat(
            paymentEventRepository
                .findByProviderAndProviderEventId(OrderProvider.SSLCOMMERZ, "evt-unknown-1")
                .orElseThrow()
                .getStatus())
        .isEqualTo(PaymentEventStatus.RECEIVED);
  }

  @Test
  void sslcommerzWebhookExpiredStatusShouldMarkFailed() throws Exception {
    var student = user("Student Incomplete", "student-payment-incomplete@example.com");
    var creator = user("Creator Incomplete", "creator-payment-incomplete@example.com");
    var course =
        course(
            "Webhook Incomplete",
            "webhook-incomplete-payment",
            creator,
            PublishStatus.PUBLISHED,
            BigDecimal.valueOf(1000));
    var order =
        order(
            student,
            OrderStatus.PENDING,
            OrderProvider.SSLCOMMERZ,
            "txn-incomplete-hook",
            BigDecimal.valueOf(1000));
    orderItem(order, course, BigDecimal.valueOf(1000), BigDecimal.ZERO);

    String payload = signedSslPayload("tran_id=txn-incomplete-hook&status=EXPIRED&val_id=val-3");

    mockMvc
        .perform(
            post("/public/webhooks/payments/sslcommerz")
                .contentType(MediaType.TEXT_PLAIN)
                .header("x-event-id", "evt-incomplete-1")
                .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.acknowledged").value(true));

    assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
        .isEqualTo(OrderStatus.FAILED);
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
  void sslcommerzWebhookReplayShouldBeIdempotent() throws Exception {
    String payload = signedSslPayload("tran_id=txn-replay&status=FAILED&val_id=val-4");

    MvcResult first =
        mockMvc
            .perform(
                post("/public/webhooks/payments/sslcommerz")
                    .contentType(MediaType.TEXT_PLAIN)
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

  private String signedSslPayload(String basePayload) {
    String verifyKey = "status,tran_id,val_id";
    String signSource = signSource(basePayload, verifyKey);
    String verifySign = md5Hex(signSource + "&store_passwd=" + md5Hex("test-password")).toUpperCase();
    return basePayload + "&verify_key=" + verifyKey + "&verify_sign=" + verifySign;
  }

  private String signSource(String payload, String verifyKey) {
    List<String> fragments = new ArrayList<>();
    String[] pairs = payload.split("&");
    for (String key : verifyKey.split(",")) {
      String match = null;
      for (String pair : pairs) {
        if (pair.startsWith(key + "=")) {
          match = pair;
          break;
        }
      }
      if (match != null) {
        fragments.add(match);
      }
    }
    fragments.sort(Comparator.naturalOrder());
    return String.join("&", fragments);
  }

  private String md5Hex(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
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

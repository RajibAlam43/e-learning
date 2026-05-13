package com.gii.api.paymentapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.OrderStatus;
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

class PaymentOrderingAndConsistencyApiIt extends AbstractPaymentApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupPaymentData();
  }

  @Test
  void failedThenSuccessShouldEndPaidAndGrantEnrollment() throws Exception {
    var student = user("Student Order1", "student-ordering-1@example.com");
    var creator = user("Creator Order1", "creator-ordering-1@example.com");
    var course =
        course(
            "Ordering Course 1",
            "ordering-course-1",
            creator,
            PublishStatus.PUBLISHED,
            BigDecimal.valueOf(1000));
    var order =
        order(
            student,
            OrderStatus.PENDING,
            OrderProvider.SSLCOMMERZ,
            "txn-ordering-1",
            BigDecimal.valueOf(1000));
    orderItem(order, course, BigDecimal.valueOf(1000), BigDecimal.ZERO);

    mockMvc
        .perform(
            get("/payments/{orderId}/failed", order.getId()).param("tran_id", "txn-ordering-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("FAILED"));

    String payload = signedSslPayload("tran_id=txn-ordering-1&status=VALID&val_id=val-ordering-1");
    mockMvc
        .perform(
            post("/public/webhooks/payments/sslcommerz")
                .contentType(MediaType.TEXT_PLAIN)
                .header("x-event-id", "evt-ordering-1")
                .content(payload))
        .andExpect(status().isOk());

    assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
        .isEqualTo(OrderStatus.PAID);
    assertThat(
            enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                student.getId(), course.getId(), EnrollmentStatus.ACTIVE))
        .isTrue();
  }

  @Test
  void cancelledThenSuccessShouldEndPaidAndGrantEnrollment() throws Exception {
    var student = user("Student Order2", "student-ordering-2@example.com");
    var creator = user("Creator Order2", "creator-ordering-2@example.com");
    var course =
        course(
            "Ordering Course 2",
            "ordering-course-2",
            creator,
            PublishStatus.PUBLISHED,
            BigDecimal.valueOf(1100));
    var order =
        order(
            student,
            OrderStatus.PENDING,
            OrderProvider.BKASH,
            "txn-ordering-2",
            BigDecimal.valueOf(1100));
    orderItem(order, course, BigDecimal.valueOf(1100), BigDecimal.ZERO);

    mockMvc
        .perform(
            get("/payments/{orderId}/cancelled", order.getId())
                .param("payment_id", "txn-ordering-2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));

    String payload =
        """
        {
          "Type":"Notification",
          "MessageId":"evt-ordering-2",
          "TopicArn":"arn:aws:sns:ap-southeast-1:123456789012:test",
          "Message":"{\\"trxID\\":\\"txn-ordering-2\\",\\"transactionStatus\\":\\"Completed\\"}",
          "Timestamp":"2018-04-19T12:22:46.236Z",
          "SignatureVersion":"1",
          "Signature":"test-signature",
          "SigningCertURL":"https://sns.ap-southeast-1.amazonaws.com/test.pem"
        }
        """;
    mockMvc
        .perform(
            post("/public/webhooks/payments/bkash")
                .contentType(MediaType.TEXT_PLAIN)
                .header("x-event-id", "evt-ordering-2")
                .header("x-amz-sns-message-type", "Notification")
                .content(payload))
        .andExpect(status().isOk());

    assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
        .isEqualTo(OrderStatus.PAID);
    assertThat(
            enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                student.getId(), course.getId(), EnrollmentStatus.ACTIVE))
        .isTrue();
  }

  @Test
  void successWithMismatchedTransactionIdShouldBeBadRequest() throws Exception {
    var student = user("Student Order3", "student-ordering-3@example.com");
    var order =
        order(
            student,
            OrderStatus.PENDING,
            OrderProvider.SSLCOMMERZ,
            "txn-ordering-3",
            BigDecimal.valueOf(500));

    mockMvc
        .perform(get("/payments/{orderId}/success", order.getId()).param("tran_id", "txn-other"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void webhookBeforeSuccessCallbackShouldStillAllowPaidTransition() throws Exception {
    var student = user("Student Order4", "student-ordering-4@example.com");
    var creator = user("Creator Order4", "creator-ordering-4@example.com");
    var course =
        course(
            "Ordering Course 4",
            "ordering-course-4",
            creator,
            PublishStatus.PUBLISHED,
            BigDecimal.valueOf(1300));
    var order =
        order(
            student,
            OrderStatus.PENDING,
            OrderProvider.SSLCOMMERZ,
            "txn-ordering-4",
            BigDecimal.valueOf(1300));
    orderItem(order, course, BigDecimal.valueOf(1300), BigDecimal.ZERO);

    String payload = signedSslPayload("tran_id=txn-ordering-4&status=VALID&val_id=val-ordering-4");
    mockMvc
        .perform(
            post("/public/webhooks/payments/sslcommerz")
                .contentType(MediaType.TEXT_PLAIN)
                .header("x-event-id", "evt-ordering-4")
                .content(payload))
        .andExpect(status().isOk());
    assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
        .isEqualTo(OrderStatus.PAID);
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
      for (String pair : pairs) {
        if (pair.startsWith(key + "=")) {
          fragments.add(pair);
          break;
        }
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

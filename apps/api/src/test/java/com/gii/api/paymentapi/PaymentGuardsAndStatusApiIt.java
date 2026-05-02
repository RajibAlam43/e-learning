package com.gii.api.paymentapi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.OrderStatus;
import com.gii.common.enums.PublishStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class PaymentGuardsAndStatusApiIt extends AbstractPaymentApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupPaymentData();
  }

  @Test
  void initiateShouldFailForNonPendingOrder() throws Exception {
    var student = user("Student NP", "student-payment-nonpending@example.com");
    var paidOrder =
        order(
            student,
            OrderStatus.PAID,
            OrderProvider.SSLCOMMERZ,
            "txn-paid-nonpending",
            BigDecimal.valueOf(500));

    mockMvc
        .perform(
            post("/payments/{orderId}/initiate", paidOrder.getId())
                .with(authentication(studentAuth(student.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"provider\":\"BKASH\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void orderStatusShouldNotLeakAcrossUsers() throws Exception {
    var owner = user("Owner A", "owner-a-payment@example.com");
    var other = user("Other B", "other-b-payment@example.com");
    var order =
        order(
            owner,
            OrderStatus.PENDING,
            OrderProvider.SSLCOMMERZ,
            "txn-owner-only",
            BigDecimal.valueOf(800));

    mockMvc
        .perform(
            get("/checkout/orders/{orderId}", order.getId())
                .with(authentication(studentAuth(other.getId()))))
        .andExpect(status().isNotFound());
  }

  @Test
  void receiptShouldBeBadRequestForPendingOrder() throws Exception {
    var student = user("Student Pending", "student-pending-receipt@example.com");
    var order =
        order(
            student,
            OrderStatus.PENDING,
            OrderProvider.SSLCOMMERZ,
            "txn-pending-receipt",
            BigDecimal.valueOf(600));

    mockMvc
        .perform(
            get("/student/orders/{orderId}/receipt", order.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void failedAndCancelledCallbacksShouldTransitionPendingOrder() throws Exception {
    var student = user("Student FC", "student-failed-cancelled@example.com");
    var failedOrder =
        order(
            student,
            OrderStatus.PENDING,
            OrderProvider.SSLCOMMERZ,
            "txn-failed-callback",
            BigDecimal.valueOf(1100));
    var cancelledOrder =
        order(
            student,
            OrderStatus.PENDING,
            OrderProvider.BKASH,
            "txn-cancelled-callback",
            BigDecimal.valueOf(900));

    mockMvc
        .perform(
            get("/payments/{orderId}/failed", failedOrder.getId())
                .param("tran_id", "txn-failed-callback"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("FAILED"))
        .andExpect(jsonPath("$.nextAction").value("INITIATE_PAYMENT"));

    mockMvc
        .perform(
            get("/payments/{orderId}/cancelled", cancelledOrder.getId())
                .param("payment_id", "txn-cancelled-callback"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"))
        .andExpect(jsonPath("$.nextAction").value("INITIATE_PAYMENT"));
  }

  @Test
  void checkoutShouldReturnNotFoundForUnpublishedCourse() throws Exception {
    var student = user("Student Draft", "student-draft-checkout@example.com");
    var creator = user("Creator Draft", "creator-draft-checkout@example.com");
    var draftCourse =
        course(
            "Draft Course",
            "draft-course-checkout-payment",
            creator,
            PublishStatus.DRAFT,
            BigDecimal.valueOf(700));

    mockMvc
        .perform(
            post("/checkout/courses/{courseId}", draftCourse.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isNotFound());
  }
}

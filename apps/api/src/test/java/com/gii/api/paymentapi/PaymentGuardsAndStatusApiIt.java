package com.gii.api.paymentapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.OrderStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.service.payment.PaidOrderEnrollmentService;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class PaymentGuardsAndStatusApiIt extends AbstractPaymentApiIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private PaidOrderEnrollmentService paidOrderEnrollmentService;

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
  void failedAndCancelledCallbacksShouldRespectProviderSpecificTransitionRules() throws Exception {
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
            get("/payments/sslcommerz/{orderId}/failed", failedOrder.getId())
                .param("tran_id", "txn-failed-callback"))
        .andExpect(status().isSeeOther())
        .andExpect(header().exists("Location"));

    mockMvc
        .perform(
            get("/payments/bkash/{orderId}/cancelled", cancelledOrder.getId())
                .param("payment_id", "txn-cancelled-callback"))
        .andExpect(status().isSeeOther())
        .andExpect(header().exists("Location"));
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
            post("/checkout/orders")
                .with(authentication(studentAuth(student.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(singleCourseCheckoutPayload(draftCourse.getId())))
        .andExpect(status().isNotFound());
  }

  @Test
  void checkoutEnforcesEnrollmentWindowAndCapacity() throws Exception {
    final var student = user("Student Policy", "student-policy-checkout@example.com");
    var creator = user("Creator Policy", "creator-policy-checkout@example.com");
    var course =
        course(
            "Policy Course",
            "policy-course-checkout",
            creator,
            PublishStatus.PUBLISHED,
            BigDecimal.valueOf(700));
    course.setEnrollmentStartsAt(java.time.Instant.now().plusSeconds(3600));
    course.setCapacity(1);
    courseRepository.saveAndFlush(course);

    mockMvc
        .perform(
            post("/checkout/orders")
                .with(authentication(studentAuth(student.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(singleCourseCheckoutPayload(course.getId())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value("Course enrollment has not opened"));

    course.setEnrollmentStartsAt(java.time.Instant.now().minusSeconds(3600));
    courseRepository.saveAndFlush(course);
    var enrolledStudent = user("Already Enrolled", "already-enrolled-policy@example.com");
    enrollmentRepository.saveAndFlush(
        com.gii.common.entity.enrollment.Enrollment.builder()
            .user(enrolledStudent)
            .course(course)
            .status(com.gii.common.enums.EnrollmentStatus.ACTIVE)
            .enrolledAt(java.time.Instant.now())
            .build());

    mockMvc
        .perform(
            post("/checkout/orders")
                .with(authentication(studentAuth(student.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(singleCourseCheckoutPayload(course.getId())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value("Course capacity has been reached"));
  }

  @Test
  void expiredEnrollmentCanBeRepurchasedAndPaidOrderReactivatesWithFreshExpiry() throws Exception {
    var student = user("Expired Student", "expired-repurchase@example.com");
    var creator = user("Expired Creator", "expired-repurchase-creator@example.com");
    var course =
        course(
            "Repurchase Course",
            "expired-repurchase-course",
            creator,
            PublishStatus.PUBLISHED,
            BigDecimal.valueOf(700));
    course.setAccessDurationDays(30);
    courseRepository.saveAndFlush(course);
    var expired = enrollment(student, course, EnrollmentStatus.ACTIVE);
    expired.setExpiresAt(java.time.Instant.now().minusSeconds(60));
    expired.setCompletedAt(java.time.Instant.now().minusSeconds(120));
    enrollmentRepository.saveAndFlush(expired);

    mockMvc
        .perform(
            post("/checkout/orders")
                .with(authentication(studentAuth(student.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(singleCourseCheckoutPayload(course.getId())))
        .andExpect(status().isOk());

    var pending =
        orderRepository.findByUserIdAndStatus(student.getId(), OrderStatus.PENDING).getFirst();
    pending.setStatus(OrderStatus.PAID);
    orderRepository.saveAndFlush(pending);
    paidOrderEnrollmentService.grant(pending.getId());

    var reactivated =
        enrollmentRepository.findByUserIdAndCourseId(student.getId(), course.getId()).orElseThrow();
    assertThat(reactivated.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
    assertThat(reactivated.getCompletedAt()).isNull();
    assertThat(reactivated.getExpiresAt())
        .isAfter(java.time.Instant.now().plusSeconds(29 * 86400L));
  }

  @Test
  void paymentInitiationRevalidatesAnExistingPendingOrder() throws Exception {
    var buyer = user("Pending Buyer", "pending-revalidate-buyer@example.com");
    final var other = user("Seat Owner", "pending-revalidate-owner@example.com");
    var creator = user("Pending Creator", "pending-revalidate-creator@example.com");
    var course =
        course(
            "Pending Revalidation",
            "pending-revalidation-course",
            creator,
            PublishStatus.PUBLISHED,
            BigDecimal.valueOf(700));
    course.setCapacity(1);
    courseRepository.saveAndFlush(course);

    mockMvc
        .perform(
            post("/checkout/orders")
                .with(authentication(studentAuth(buyer.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(singleCourseCheckoutPayload(course.getId())))
        .andExpect(status().isOk());
    var pending =
        orderRepository.findByUserIdAndStatus(buyer.getId(), OrderStatus.PENDING).getFirst();
    enrollment(other, course, EnrollmentStatus.ACTIVE);

    mockMvc
        .perform(
            post("/payments/{orderId}/initiate", pending.getId())
                .with(authentication(studentAuth(buyer.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"provider\":\"SSLCOMMERZ\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value("Course capacity has been reached"));
  }

  private String singleCourseCheckoutPayload(java.util.UUID courseId) {
    return """
        {
          "items": [
            {"itemType":"COURSE","courseId":"%s"}
          ]
        }
        """
        .formatted(courseId);
  }
}

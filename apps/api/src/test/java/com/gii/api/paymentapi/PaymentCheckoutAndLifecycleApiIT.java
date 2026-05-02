package com.gii.api.paymentapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.OrderStatus;
import com.gii.common.enums.PublishStatus;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

class PaymentCheckoutAndLifecycleApiIt extends AbstractPaymentApiIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private EntityManager entityManager;

  @AfterEach
  void cleanup() {
    cleanupPaymentData();
  }

  @Test
  void createPendingOrderShouldCreateAndReuseUnexpiredPendingOrder() throws Exception {
    var student = user("Student One", "student-payment-a@example.com");
    var creator = user("Creator One", "creator-payment-a@example.com");
    var course =
        course(
            "Spring Course",
            "spring-course-payment-a",
            creator,
            PublishStatus.PUBLISHED,
            BigDecimal.valueOf(1200));

    mockMvc
        .perform(
            post("/checkout/courses/{courseId}", course.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.totalAmount").value(1200));

    mockMvc
        .perform(
            post("/checkout/courses/{courseId}", course.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING"));

    assertThat(orderRepository.findByUserIdAndStatus(student.getId(), OrderStatus.PENDING))
        .hasSize(1);
  }

  @Test
  void createPendingOrderShouldReturnConflictWhenAlreadyEnrolled() throws Exception {
    var student = user("Student Enrolled", "student-payment-enrolled@example.com");
    var creator = user("Creator Enrolled", "creator-payment-enrolled@example.com");
    var course =
        course(
            "Already Enrolled Course",
            "already-enrolled-course-payment",
            creator,
            PublishStatus.PUBLISHED,
            BigDecimal.valueOf(900));
    enrollment(student, course, EnrollmentStatus.ACTIVE);

    mockMvc
        .perform(
            post("/checkout/courses/{courseId}", course.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isConflict());
  }

  @Test
  void initiateSuccessCallbackStatusAndReceiptShouldPersistAndReturnExpectedState()
      throws Exception {
    var student = user("Student Life", "student-payment-life@example.com");
    var creator = user("Creator Life", "creator-payment-life@example.com");
    var course =
        course(
            "Lifecycle Course",
            "lifecycle-course-payment",
            creator,
            PublishStatus.PUBLISHED,
            BigDecimal.valueOf(1500));

    mockMvc
        .perform(
            post("/checkout/courses/{courseId}", course.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk());

    var order =
        orderRepository.findByUserIdAndStatus(student.getId(), OrderStatus.PENDING).getFirst();

    mockMvc
        .perform(
            post("/payments/{orderId}/initiate", order.getId())
                .with(authentication(studentAuth(student.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"provider\":\"BKASH\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.provider").value("BKASH"));

    var initiatedOrder = orderRepository.findById(order.getId()).orElseThrow();

    mockMvc
        .perform(
            get("/payments/{orderId}/success", order.getId())
                .param("tran_id", initiatedOrder.getProviderTxnId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAID"))
        .andExpect(jsonPath("$.coursesEnrolled").value(true));

    mockMvc
        .perform(
            get("/checkout/orders/{orderId}", order.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAID"))
        .andExpect(jsonPath("$.nextAction").value("REDIRECT_TO_DASHBOARD"));

    mockMvc
        .perform(
            get("/student/orders/{orderId}/receipt", order.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.orderStatus").value("PAID"))
        .andExpect(jsonPath("$.items[0].courseSlug").value("lifecycle-course-payment"));

    assertThat(
            enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                student.getId(), course.getId(), EnrollmentStatus.ACTIVE))
        .isTrue();
  }

  @Test
  @Transactional
  void initiatePaymentShouldFailWhenOrderExpired() throws Exception {
    var student = user("Student Expired", "student-payment-expired@example.com");
    var order =
        order(
            student,
            OrderStatus.PENDING,
            OrderProvider.SSLCOMMERZ,
            "txn-expired",
            BigDecimal.valueOf(500));
    entityManager
        .createNativeQuery("UPDATE orders SET created_at = :createdAt WHERE id = :id")
        .setParameter("createdAt", Instant.now().minusSeconds(1900))
        .setParameter("id", order.getId())
        .executeUpdate();
    entityManager.clear();

    mockMvc
        .perform(
            post("/payments/{orderId}/initiate", order.getId())
                .with(authentication(studentAuth(student.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"provider\":\"SSLCOMMERZ\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @Transactional
  void initiatePaymentShouldSucceedNearExpiryBoundary() throws Exception {
    var student = user("Student Boundary", "student-payment-boundary@example.com");
    var order =
        order(
            student,
            OrderStatus.PENDING,
            OrderProvider.SSLCOMMERZ,
            "txn-boundary",
            BigDecimal.valueOf(500));
    entityManager
        .createNativeQuery("UPDATE orders SET created_at = :createdAt WHERE id = :id")
        .setParameter("createdAt", Instant.now().minusSeconds(1790))
        .setParameter("id", order.getId())
        .executeUpdate();
    entityManager.clear();

    mockMvc
        .perform(
            post("/payments/{orderId}/initiate", order.getId())
                .with(authentication(studentAuth(student.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"provider\":\"BKASH\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.provider").value("BKASH"));
  }

  @Test
  void paymentSuccessShouldRequireProviderTransactionIdentifier() throws Exception {
    var student = user("Student Callback", "student-payment-callback@example.com");
    var order =
        order(
            student,
            OrderStatus.PENDING,
            OrderProvider.SSLCOMMERZ,
            "txn-callback",
            BigDecimal.valueOf(700));

    mockMvc
        .perform(get("/payments/{orderId}/success", order.getId()))
        .andExpect(status().isBadRequest());
  }
}

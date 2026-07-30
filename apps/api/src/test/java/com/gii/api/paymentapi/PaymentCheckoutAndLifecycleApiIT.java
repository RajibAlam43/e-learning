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

  private String singleCollectionCheckoutPayload(java.util.UUID collectionId) {
    return """
        {
          "items": [
            {"itemType":"COLLECTION","collectionId":"%s"}
          ]
        }
        """
        .formatted(collectionId);
  }

  private String mixedCheckoutPayload(
      java.util.UUID courseId, java.util.UUID collectionId) {
    return """
        {
          "items": [
            {"itemType":"COURSE","courseId":"%s"},
            {"itemType":"COLLECTION","collectionId":"%s"}
          ]
        }
        """
        .formatted(courseId, collectionId);
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
    course.setTitleEn("Spring Course English");
    courseRepository.saveAndFlush(course);

    mockMvc
        .perform(
            post("/checkout/orders")
                .with(authentication(studentAuth(student.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(singleCourseCheckoutPayload(course.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.totalAmount").value(1200));

    mockMvc
        .perform(
            post("/checkout/orders")
                .param("lang", "en")
                .with(authentication(studentAuth(student.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(singleCourseCheckoutPayload(course.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.items[0].courseName").value("Spring Course English"));

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
            post("/checkout/orders")
                .with(authentication(studentAuth(student.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(singleCourseCheckoutPayload(course.getId())))
        .andExpect(status().isConflict());
  }

  @Test
  void createPendingOrderShouldApplyOwnedIncludedCourseDiscountForCollection() throws Exception {
    var student = user("Student Discount", "student-payment-discount@example.com");
    var creator = user("Creator Discount", "creator-payment-discount@example.com");
    var ownedCourse =
        course(
            "Owned Course",
            "owned-course-discount-payment",
            creator,
            PublishStatus.PUBLISHED,
            BigDecimal.valueOf(900));
    var otherCourse =
        course(
            "Other Course",
            "other-course-discount-payment",
            creator,
            PublishStatus.PUBLISHED,
            BigDecimal.valueOf(700));
    enrollment(student, ownedCourse, EnrollmentStatus.ACTIVE);

    var collection =
        collection(
            "Discount Collection",
            "discount-collection-payment",
            creator,
            PublishStatus.PUBLISHED,
            BigDecimal.valueOf(2000));
    collectionCourse(collection, ownedCourse, 1, true);
    collectionCourse(collection, otherCourse, 2, true);

    mockMvc
        .perform(
            post("/checkout/orders")
                .with(authentication(studentAuth(student.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(singleCollectionCheckoutPayload(collection.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.subtotal").value(2000))
        .andExpect(jsonPath("$.totalDiscount").value(900))
        .andExpect(jsonPath("$.totalAmount").value(1100))
        .andExpect(jsonPath("$.items[0].discountReason").value("ALREADY_OWNED_INCLUDED_COURSES"));
  }

  @Test
  void createPendingOrderShouldBlockCourseWhenIncludedInSelectedCollection() throws Exception {
    var student = user("Student Overlap", "student-payment-overlap@example.com");
    var creator = user("Creator Overlap", "creator-payment-overlap@example.com");
    var courseInCollection =
        course(
            "Overlap Course",
            "overlap-course-payment",
            creator,
            PublishStatus.PUBLISHED,
            BigDecimal.valueOf(1000));
    var collection =
        collection(
            "Overlap Collection",
            "overlap-collection-payment",
            creator,
            PublishStatus.PUBLISHED,
            BigDecimal.valueOf(1800));
    collectionCourse(collection, courseInCollection, 1, true);

    mockMvc
        .perform(
            post("/checkout/orders")
                .with(authentication(studentAuth(student.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mixedCheckoutPayload(courseInCollection.getId(), collection.getId())))
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
            post("/checkout/orders")
                .with(authentication(studentAuth(student.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(singleCourseCheckoutPayload(course.getId())))
        .andExpect(status().isOk());

    var order =
        orderRepository.findByUserIdAndStatus(student.getId(), OrderStatus.PENDING).getFirst();

    order.setProvider(OrderProvider.BKASH);
    order.setProviderTxnId("bkash-lifecycle-txn");
    orderRepository.save(order);
    var initiatedOrder = orderRepository.findById(order.getId()).orElseThrow();

    String payload =
        """
        {
          "Type":"Notification",
          "MessageId":"evt-lifecycle-success",
          "TopicArn":"arn:aws:sns:ap-southeast-1:123456789012:test",
          "Message":"{\\"trxID\\":\\"%s\\",\\"transactionStatus\\":\\"Completed\\"}",
          "Timestamp":"2018-04-19T12:22:46.236Z",
          "SignatureVersion":"1",
          "Signature":"test-signature",
          "SigningCertURL":"https://sns.ap-southeast-1.amazonaws.com/test.pem"
        }
        """
            .formatted(initiatedOrder.getProviderTxnId());
    mockMvc
        .perform(
            post("/public/webhooks/payments/bkash")
                .contentType(MediaType.TEXT_PLAIN)
                .header("x-event-id", "evt-lifecycle-success")
                .header("x-amz-sns-message-type", "Notification")
                .content(payload))
        .andExpect(status().isOk());

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
  void paidMixedCartShouldGrantCourseAndCollectionEnrollmentsAndShowBothInReceipt()
      throws Exception {
    var student = user("Student Mixed", "student-payment-mixed@example.com");
    var creator = user("Creator Mixed", "creator-payment-mixed@example.com");
    var standaloneCourse =
        course(
            "Standalone Course",
            "standalone-course-payment",
            creator,
            PublishStatus.PUBLISHED,
            BigDecimal.valueOf(1200));
    var collectionCourseOne =
        course(
            "Collection Course 1",
            "collection-course-1-payment",
            creator,
            PublishStatus.PUBLISHED,
            BigDecimal.valueOf(800));
    var collectionCourseTwo =
        course(
            "Collection Course 2",
            "collection-course-2-payment",
            creator,
            PublishStatus.PUBLISHED,
            BigDecimal.valueOf(900));
    var collection =
        collection(
            "Mixed Collection",
            "mixed-collection-payment",
            creator,
            PublishStatus.PUBLISHED,
            BigDecimal.valueOf(2200));
    collectionCourse(collection, collectionCourseOne, 1, true);
    collectionCourse(collection, collectionCourseTwo, 2, true);

    mockMvc
        .perform(
            post("/checkout/orders")
                .with(authentication(studentAuth(student.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mixedCheckoutPayload(standaloneCourse.getId(), collection.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalAmount").value(3400));

    var order =
        orderRepository.findByUserIdAndStatus(student.getId(), OrderStatus.PENDING).getFirst();
    order.setProvider(OrderProvider.BKASH);
    order.setProviderTxnId("bkash-mixed-cart-txn");
    orderRepository.save(order);

    String payload =
        """
        {
          "Type":"Notification",
          "MessageId":"evt-mixed-cart-success",
          "TopicArn":"arn:aws:sns:ap-southeast-1:123456789012:test",
          "Message":"{\\"trxID\\":\\"%s\\",\\"transactionStatus\\":\\"Completed\\"}",
          "Timestamp":"2018-04-19T12:22:46.236Z",
          "SignatureVersion":"1",
          "Signature":"test-signature",
          "SigningCertURL":"https://sns.ap-southeast-1.amazonaws.com/test.pem"
        }
        """
            .formatted(order.getProviderTxnId());
    mockMvc
        .perform(
            post("/public/webhooks/payments/bkash")
                .contentType(MediaType.TEXT_PLAIN)
                .header("x-event-id", "evt-mixed-cart-success")
                .header("x-amz-sns-message-type", "Notification")
                .content(payload))
        .andExpect(status().isOk());

    assertThat(
            enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                student.getId(), standaloneCourse.getId(), EnrollmentStatus.ACTIVE))
        .isTrue();
    assertThat(
            enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                student.getId(), collectionCourseOne.getId(), EnrollmentStatus.ACTIVE))
        .isTrue();
    assertThat(
            enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                student.getId(), collectionCourseTwo.getId(), EnrollmentStatus.ACTIVE))
        .isTrue();
    assertThat(
            collectionEnrollmentRepository.existsByUserIdAndCollectionIdAndStatus(
                student.getId(), collection.getId(), EnrollmentStatus.ACTIVE))
        .isTrue();

    mockMvc
        .perform(
            get("/student/orders/{orderId}/receipt", order.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.items[?(@.itemSlug=='standalone-course-payment')]").isNotEmpty())
        .andExpect(jsonPath("$.items[?(@.itemSlug=='mixed-collection-payment')]").isNotEmpty());
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
                .content("{\"provider\":\"SSLCOMMERZ\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.provider").value("SSLCOMMERZ"));
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
        .perform(get("/payments/sslcommerz/{orderId}/success", order.getId()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void paymentFailedShouldRequireProviderTransactionIdentifier() throws Exception {
    var student = user("Student Failed Callback", "student-payment-failed-cb@example.com");
    var order =
        order(
            student,
            OrderStatus.PENDING,
            OrderProvider.SSLCOMMERZ,
            "txn-failed-callback",
            BigDecimal.valueOf(700));

    mockMvc
        .perform(get("/payments/sslcommerz/{orderId}/failed", order.getId()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void paymentCancelledShouldRequireProviderTransactionIdentifier() throws Exception {
    var student = user("Student Cancel Callback", "student-payment-cancel-cb@example.com");
    var order =
        order(
            student,
            OrderStatus.PENDING,
            OrderProvider.SSLCOMMERZ,
            "txn-cancel-callback",
            BigDecimal.valueOf(700));

    mockMvc
        .perform(get("/payments/sslcommerz/{orderId}/cancelled", order.getId()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void bkashSuccessShouldAcceptPaymentIdAlias() throws Exception {
    var student = user("Student Alias", "student-payment-alias@example.com");
    var order =
        order(
            student,
            OrderStatus.PENDING,
            OrderProvider.BKASH,
            "bkash-payment-xyz",
            BigDecimal.valueOf(700));

    mockMvc
        .perform(
            get("/payments/bkash/{orderId}/success", order.getId())
                .param("paymentID", "bkash-payment-xyz")
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isBadRequest());
  }

}

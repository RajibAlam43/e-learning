package com.gii.api.paymentapi;

import com.gii.common.entity.course.Course;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.order.Order;
import com.gii.common.entity.order.OrderItem;
import com.gii.common.entity.order.PaymentEvent;
import com.gii.common.entity.user.User;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.OrderStatus;
import com.gii.common.enums.PaymentEventStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.StudyMode;
import com.gii.common.enums.UserStatus;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.order.OrderItemRepository;
import com.gii.common.repository.order.OrderRepository;
import com.gii.common.repository.order.PaymentEventRepository;
import com.gii.common.repository.user.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

abstract class PaymentApiTestSupport {

  @Autowired protected UserRepository userRepository;
  @Autowired protected CourseRepository courseRepository;
  @Autowired protected EnrollmentRepository enrollmentRepository;
  @Autowired protected OrderRepository orderRepository;
  @Autowired protected OrderItemRepository orderItemRepository;
  @Autowired protected PaymentEventRepository paymentEventRepository;

  protected void cleanupPaymentData() {
    paymentEventRepository.deleteAll();
    enrollmentRepository.deleteAll();
    orderItemRepository.deleteAll();
    orderRepository.deleteAll();
    courseRepository.deleteAll();
    userRepository.deleteAll();
  }

  protected Authentication studentAuth(UUID userId) {
    return new UsernamePasswordAuthenticationToken(
        userId, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
  }

  protected Authentication adminAuth(UUID userId) {
    return new UsernamePasswordAuthenticationToken(
        userId, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
  }

  protected User user(String fullName, String email) {
    return userRepository.save(
        User.builder()
            .fullName(fullName)
            .email(email)
            .passwordHash("x")
            .status(UserStatus.ACTIVE)
            .build());
  }

  protected Course course(
      String title, String slug, User creator, PublishStatus status, BigDecimal price) {
    return courseRepository.save(
        Course.builder()
            .title(title)
            .slug(slug)
            .priceBdt(price)
            .isFree(price.compareTo(BigDecimal.ZERO) == 0)
            .level(CourseLevel.BEGINNER)
            .language(CourseLanguage.EN)
            .studyMode(StudyMode.SCHEDULED)
            .status(status)
            .publishedAt(status == PublishStatus.PUBLISHED ? Instant.now() : null)
            .liveSessionCount(0)
            .quizCount(0)
            .recordedHoursCount(0)
            .createdBy(creator)
            .build());
  }

  protected Order order(
      User user, OrderStatus status, OrderProvider provider, String txnId, BigDecimal amount) {
    return orderRepository.save(
        Order.builder()
            .user(user)
            .status(status)
            .provider(provider)
            .providerTxnId(txnId)
            .amountBdt(amount)
            .currency("BDT")
            .paidAt(status == OrderStatus.PAID ? Instant.now() : null)
            .build());
  }

  protected OrderItem orderItem(Order order, Course course, BigDecimal price, BigDecimal discount) {
    return orderItemRepository.save(
        OrderItem.builder()
            .order(order)
            .course(course)
            .priceBdt(price)
            .discountBdt(discount)
            .build());
  }

  protected Enrollment enrollment(User user, Course course, EnrollmentStatus status) {
    return enrollmentRepository.save(
        Enrollment.builder()
            .user(user)
            .course(course)
            .status(status)
            .enrolledAt(Instant.now())
            .build());
  }

  protected PaymentEvent paymentEvent(
      Order order, OrderProvider provider, String eventId, PaymentEventStatus status) {
    return paymentEventRepository.save(
        PaymentEvent.builder()
            .order(order)
            .provider(provider)
            .eventType("webhook")
            .providerEventId(eventId)
            .rawPayloadJson(java.util.Map.of("k", "v"))
            .status(status)
            .processedAt(Instant.now())
            .build());
  }
}

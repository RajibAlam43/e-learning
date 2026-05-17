package com.gii.api.paymentapi;

import com.gii.common.entity.collection.Collection;
import com.gii.common.entity.collection.CollectionCourse;
import com.gii.common.entity.collection.CollectionCourseId;
import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.order.Order;
import com.gii.common.entity.order.OrderItem;
import com.gii.common.entity.order.PaymentEvent;
import com.gii.common.entity.user.User;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.CollectionType;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.OrderItemType;
import com.gii.common.enums.OrderStatus;
import com.gii.common.enums.PaymentEventStatus;
import com.gii.common.enums.PaymentEventType;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.StudyMode;
import com.gii.common.enums.UserStatus;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.collection.CollectionCourseRepository;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import com.gii.common.repository.collection.CollectionRepository;
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
  @Autowired protected CollectionRepository collectionRepository;
  @Autowired protected CollectionCourseRepository collectionCourseRepository;
  @Autowired protected CollectionEnrollmentRepository collectionEnrollmentRepository;
  @Autowired protected CourseRepository courseRepository;
  @Autowired protected EnrollmentRepository enrollmentRepository;
  @Autowired protected OrderRepository orderRepository;
  @Autowired protected OrderItemRepository orderItemRepository;
  @Autowired protected PaymentEventRepository paymentEventRepository;

  protected void cleanupPaymentData() {
    paymentEventRepository.deleteAll();
    collectionEnrollmentRepository.deleteAll();
    enrollmentRepository.deleteAll();
    orderItemRepository.deleteAll();
    orderRepository.deleteAll();
    collectionCourseRepository.deleteAll();
    collectionRepository.deleteAll();
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
            .itemType(OrderItemType.COURSE)
            .course(course)
            .titleSnapshot(course.getTitle())
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

  protected Collection collection(
      String title, String slug, User creator, PublishStatus status, BigDecimal price) {
    return collectionRepository.save(
        Collection.builder()
            .title(title)
            .slug(slug)
            .type(CollectionType.PACK)
            .priceBdt(price)
            .status(status)
            .publishedAt(status == PublishStatus.PUBLISHED ? Instant.now() : null)
            .createdBy(creator)
            .build());
  }

  protected CollectionCourse collectionCourse(
      Collection collection, Course course, int position, boolean isMandatory) {
    return collectionCourseRepository.save(
        CollectionCourse.builder()
            .id(
                CollectionCourseId.builder()
                    .collectionId(collection.getId())
                    .courseId(course.getId())
                    .build())
            .collection(collection)
            .course(course)
            .position(position)
            .isMandatory(isMandatory)
            .build());
  }

  protected CollectionEnrollment collectionEnrollment(
      User user, Collection collection, EnrollmentStatus status) {
    return collectionEnrollmentRepository.save(
        CollectionEnrollment.builder()
            .user(user)
            .collection(collection)
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
            .eventType(PaymentEventType.BKASH_WEBHOOK)
            .providerEventId(eventId)
            .rawPayloadJson(java.util.Map.of("k", "v"))
            .status(status)
            .processedAt(Instant.now())
            .build());
  }
}

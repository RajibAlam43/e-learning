package com.gii.api.service.payment;

import com.gii.api.model.response.payment.CheckoutOrderItemResponse;
import com.gii.api.model.response.payment.CheckoutOrderResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.order.Order;
import com.gii.common.entity.order.OrderItem;
import com.gii.common.entity.user.User;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.OrderStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.order.OrderItemRepository;
import com.gii.common.repository.order.OrderRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class PendingOrderService {

  private static final long ORDER_EXPIRY_SECONDS =
          Duration.ofMinutes(30).getSeconds();

  private final CurrentUserService currentUserService;
  private final CourseRepository courseRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;

  public CheckoutOrderResponse execute(UUID courseId, Authentication authentication) {
    User user = currentUserService.getCurrentUser(authentication);
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

    if (course.getStatus() != PublishStatus.PUBLISHED) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
    }

    if (enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
        user.getId(), courseId, EnrollmentStatus.ACTIVE)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Already enrolled in this course");
    }

    // Reuse an unexpired pending order for the same user+course to avoid duplicate checkout rows.
    Order existingPendingOrder = findReusablePendingOrder(user.getId(), courseId);
    if (existingPendingOrder != null) {
      return toCheckoutResponse(existingPendingOrder, course, user);
    }

    BigDecimal price =
        course.getIsFree() != null && course.getIsFree() ? BigDecimal.ZERO : course.getPriceBdt();
    Order order =
        Order.builder()
            .user(user)
            .amountBdt(price)
            .currency("BDT")
            // Provider is selected at initiation.
            // Placeholder keeps current schema constraints satisfied.
            .provider(OrderProvider.SSLCOMMERZ)
            .status(OrderStatus.PENDING)
            .build();
    Order savedOrder = orderRepository.save(order);

    OrderItem item =
        OrderItem.builder()
            .order(savedOrder)
            .course(course)
            .priceBdt(price)
            .discountBdt(BigDecimal.ZERO)
            .build();
    orderItemRepository.save(item);

    return toCheckoutResponse(savedOrder, course, user);
  }

  private Order findReusablePendingOrder(UUID userId, UUID courseId) {
    Instant now = Instant.now();
    return orderRepository.findByUserIdAndStatus(userId, OrderStatus.PENDING).stream()
        .filter(order -> order.getCreatedAt().plusSeconds(ORDER_EXPIRY_SECONDS).isAfter(now))
        .filter(
            order ->
                orderItemRepository.findByOrderId(order.getId()).stream()
                    .anyMatch(item -> item.getCourse().getId().equals(courseId)))
        .findFirst()
        .orElse(null);
  }

  private CheckoutOrderResponse toCheckoutResponse(Order order, Course course, User user) {
    Instant expiresAt = order.getCreatedAt().plusSeconds(ORDER_EXPIRY_SECONDS);
    BigDecimal subtotal = order.getAmountBdt();
    BigDecimal totalDiscount = BigDecimal.ZERO;
    BigDecimal totalAmount = subtotal.subtract(totalDiscount);
    CheckoutOrderItemResponse item =
        CheckoutOrderItemResponse.builder()
            .courseId(course.getId())
            .courseName(course.getTitle())
            .courseSlug(course.getSlug())
            .courseThumbnailUrl(course.getThumbnailUrl())
            .originalPrice(subtotal)
            .discountAmount(totalDiscount)
            .finalPrice(totalAmount)
            .discountReason(null)
            .build();
    return CheckoutOrderResponse.builder()
        .orderId(order.getId())
        .subtotal(subtotal)
        .totalDiscount(totalDiscount)
        .totalAmount(totalAmount)
        .currency(order.getCurrency())
        .items(List.of(item))
        .status(order.getStatus())
        .expiresAt(expiresAt)
        .isExpired(Instant.now().isAfter(expiresAt))
        .customerEmail(user.getEmail())
        .customerPhone(user.getPhone())
        .nextAction("INITIATE_PAYMENT")
        .build();
  }
}

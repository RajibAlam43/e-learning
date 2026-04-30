package com.gii.api.service.student;

import com.gii.api.model.response.student.OrderItemSummaryResponse;
import com.gii.api.model.response.student.StudentOrderSummaryResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.entity.order.Order;
import com.gii.common.entity.order.OrderItem;
import com.gii.common.entity.user.User;
import com.gii.common.repository.order.OrderItemRepository;
import com.gii.common.repository.order.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentOrdersService {

  private final CurrentUserService currentUserService;
  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;

  public List<StudentOrderSummaryResponse> execute(Authentication authentication) {
    User user = currentUserService.getCurrentUser(authentication);
    List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

    return orders.stream().map(this::toOrderSummary).toList();
  }

  private StudentOrderSummaryResponse toOrderSummary(Order order) {
    List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
    List<OrderItemSummaryResponse> itemResponses =
        items.stream()
            .map(
                item -> {
                  BigDecimal finalAmount = item.getPriceBdt().subtract(item.getDiscountBdt());
                  return OrderItemSummaryResponse.builder()
                      .courseId(item.getCourse().getId())
                      .courseName(item.getCourse().getTitle())
                      .priceBdt(item.getPriceBdt())
                      .discountBdt(item.getDiscountBdt())
                      .finalAmount(finalAmount)
                      .build();
                })
            .toList();

    return StudentOrderSummaryResponse.builder()
        .orderId(order.getId())
        .status(order.getStatus())
        .totalAmount(order.getAmountBdt())
        .currency(order.getCurrency())
        .provider(order.getProvider())
        .courseCount(items.size())
        .items(itemResponses)
        .createdAt(order.getCreatedAt())
        .paidAt(order.getPaidAt())
        .refundedAt(order.getRefundedAt())
        .build();
  }
}

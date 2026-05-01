package com.gii.api.service.admin;

import com.gii.api.model.request.admin.UpdateOrderRequest;
import com.gii.api.model.response.admin.AdminOrderDetailResponse;
import com.gii.api.model.response.admin.AdminOrderItemResponse;
import com.gii.api.model.response.admin.AdminOrderSummaryResponse;
import com.gii.common.entity.order.Order;
import com.gii.common.entity.order.OrderItem;
import com.gii.common.enums.OrderStatus;
import com.gii.common.repository.order.OrderItemRepository;
import com.gii.common.repository.order.OrderRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminOrderManagementService {

  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;

  @Transactional(readOnly = true)
  public List<AdminOrderSummaryResponse> list() {
    return orderRepository.findAll().stream().map(this::toSummary).toList();
  }

  @Transactional(readOnly = true)
  public AdminOrderDetailResponse get(UUID orderId) {
    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    return toDetail(order);
  }

  public AdminOrderDetailResponse update(UUID orderId, UpdateOrderRequest request) {
    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    if (request.status() != null) {
      OrderStatus newStatus = parseStatus(request.status());
      order.setStatus(newStatus);
      if (newStatus == OrderStatus.PAID && order.getPaidAt() == null) {
        order.setPaidAt(Instant.now());
      }
      if (newStatus == OrderStatus.REFUNDED && order.getRefundedAt() == null) {
        order.setRefundedAt(Instant.now());
      }
    }
    // adminNote is accepted in API but not persisted until entity adds dedicated column.
    Order saved = orderRepository.save(order);
    return toDetail(saved, request.adminNote());
  }

  private AdminOrderSummaryResponse toSummary(Order order) {
    return AdminOrderSummaryResponse.builder()
        .orderId(order.getId())
        .customerName(order.getUser().getFullName())
        .customerEmail(order.getUser().getEmail())
        .totalAmount(order.getAmountBdt())
        .status(order.getStatus().name())
        .provider(order.getProvider().name())
        .createdAt(order.getCreatedAt())
        .paidAt(order.getPaidAt())
        .build();
  }

  private AdminOrderDetailResponse toDetail(Order order) {
    return toDetail(order, null);
  }

  private AdminOrderDetailResponse toDetail(Order order, String adminNote) {
    List<AdminOrderItemResponse> items =
        orderItemRepository.findByOrderId(order.getId()).stream()
            .map(this::toItemResponse)
            .toList();
    return AdminOrderDetailResponse.builder()
        .orderId(order.getId())
        .userId(order.getUser().getId())
        .customerName(order.getUser().getFullName())
        .customerEmail(order.getUser().getEmail())
        .customerPhone(order.getUser().getPhone())
        .totalAmount(order.getAmountBdt())
        .currency(order.getCurrency())
        .provider(order.getProvider().name())
        .providerTxnId(order.getProviderTxnId())
        .status(order.getStatus().name())
        .paidAt(order.getPaidAt())
        .refundedAt(order.getRefundedAt())
        .createdAt(order.getCreatedAt())
        .updatedAt(order.getCreatedAt())
        .adminNote(adminNote)
        .items(items)
        .build();
  }

  private AdminOrderItemResponse toItemResponse(OrderItem item) {
    return AdminOrderItemResponse.builder()
        .courseId(item.getCourse().getId())
        .courseName(item.getCourse().getTitle())
        .priceBdt(item.getPriceBdt())
        .discountBdt(item.getDiscountBdt())
        .finalAmount(item.getPriceBdt().subtract(item.getDiscountBdt()))
        .build();
  }

  private OrderStatus parseStatus(String value) {
    try {
      return OrderStatus.valueOf(value.trim().toUpperCase());
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order status");
    }
  }
}

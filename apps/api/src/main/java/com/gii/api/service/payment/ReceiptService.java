package com.gii.api.service.payment;

import com.gii.api.model.response.payment.ReceiptItemResponse;
import com.gii.api.model.response.payment.ReceiptResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.entity.order.Order;
import com.gii.common.entity.order.OrderItem;
import com.gii.common.enums.OrderStatus;
import com.gii.common.repository.order.OrderItemRepository;
import com.gii.common.repository.order.OrderRepository;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
@Transactional(readOnly = true)
public class ReceiptService {

  private final CurrentUserService currentUserService;
  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;

  public ReceiptResponse execute(UUID orderId, Authentication authentication) {
    UUID userId = currentUserService.getCurrentUserId(authentication);
    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    if (!order.getUser().getId().equals(userId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not order owner");
    }
    if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.REFUNDED) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Receipt is only available for paid orders");
    }

    List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
    List<ReceiptItemResponse> receiptItems =
        items.stream()
            .map(
                item ->
                    ReceiptItemResponse.builder()
                        .courseId(item.getCourse().getId())
                        .courseName(item.getCourse().getTitle())
                        .courseSlug(item.getCourse().getSlug())
                        .unitPrice(item.getPriceBdt())
                        .discountAmount(item.getDiscountBdt())
                        .lineTotal(item.getPriceBdt().subtract(item.getDiscountBdt()))
                        .accessStartDate(
                            order.getPaidAt() != null ? order.getPaidAt().toString() : null)
                        .accessExpiryDate(null)
                        .build())
            .toList();

    BigDecimal subtotal =
        items.stream().map(OrderItem::getPriceBdt).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal discount =
        items.stream().map(OrderItem::getDiscountBdt).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal finalAmount = subtotal.subtract(discount);

    return ReceiptResponse.builder()
        .orderId(order.getId())
        .receiptNumber(receiptNumber(order))
        .orderStatus(order.getStatus())
        .totalAmount(order.getAmountBdt())
        .currency(order.getCurrency())
        .paymentProvider(order.getProvider())
        .providerTransactionId(order.getProviderTxnId())
        .customerName(order.getUser().getFullName())
        .customerEmail(order.getUser().getEmail())
        .customerPhone(order.getUser().getPhone())
        .items(receiptItems)
        .subtotal(subtotal)
        .totalDiscount(discount)
        .finalAmount(finalAmount)
        .orderDate(order.getCreatedAt())
        .paidDate(order.getPaidAt())
        .pdfReceiptUrl(null)
        .receiptPageUrl("/student/orders/" + order.getId() + "/receipt")
        .supportEmail("support@gii.com")
        .supportPhone(null)
        .footerMessage("Thank you for learning with us.")
        .build();
  }

  private String receiptNumber(Order order) {
    String day =
        DateTimeFormatter.ofPattern("yyyyMMdd")
            .withZone(ZoneOffset.UTC)
            .format(order.getCreatedAt());
    String suffix = order.getId().toString().substring(0, 8).toUpperCase();
    return "INV-" + day + "-" + suffix;
  }
}

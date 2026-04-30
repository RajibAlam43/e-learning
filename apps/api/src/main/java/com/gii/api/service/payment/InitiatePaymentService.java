package com.gii.api.service.payment;

import com.gii.api.model.request.payment.InitiatePaymentRequest;
import com.gii.api.model.response.payment.PaymentInitiationResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.entity.order.Order;
import com.gii.common.entity.user.User;
import com.gii.common.enums.OrderStatus;
import com.gii.common.repository.order.OrderRepository;
import java.time.Instant;
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
public class InitiatePaymentService {

  private static final long PAYMENT_TIMEOUT_SECONDS = 20 * 60;
  private static final long ORDER_EXPIRY_SECONDS = 30 * 60;

  private final CurrentUserService currentUserService;
  private final OrderRepository orderRepository;

  public PaymentInitiationResponse execute(
      UUID orderId, InitiatePaymentRequest request, Authentication authentication) {
    User user = currentUserService.getCurrentUser(authentication);
    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

    if (!order.getUser().getId().equals(user.getId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not order owner");
    }
    if (order.getStatus() != OrderStatus.PENDING) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order is not payable");
    }
    if (order.getCreatedAt().plusSeconds(ORDER_EXPIRY_SECONDS).isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order has expired");
    }

    String sessionId = "pay_" + UUID.randomUUID();
    order.setProvider(request.provider());
    order.setProviderTxnId(sessionId);
    orderRepository.save(order);

    String redirectUrl =
        "/payments/" + order.getId() + "/gateway/" + request.provider().name().toLowerCase();
    return PaymentInitiationResponse.builder()
        .orderId(order.getId())
        .provider(order.getProvider())
        .sessionId(sessionId)
        .redirectUrl(redirectUrl)
        .gatewayName(order.getProvider().name())
        .paymentUrl(redirectUrl)
        .timeoutSeconds(PAYMENT_TIMEOUT_SECONDS)
        .providerTransactionId(sessionId)
        .providerReference("ORDER-" + order.getId())
        .successCallbackUrl("/payments/" + order.getId() + "/success")
        .failureCallbackUrl("/payments/" + order.getId() + "/failed")
        .build();
  }
}

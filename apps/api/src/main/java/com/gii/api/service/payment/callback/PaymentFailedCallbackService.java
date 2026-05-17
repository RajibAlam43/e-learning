package com.gii.api.service.payment.callback;

import com.gii.api.service.payment.PaymentFlowSupportService;
import com.gii.common.entity.order.Order;
import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.PaymentEventStatus;
import com.gii.common.enums.PaymentEventType;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PaymentFailedCallbackService {

  private final PaymentFlowSupportService paymentFlowSupportService;

  public void execute(UUID orderId, Map<String, String> queryParams) {
    String providerEventId =
        paymentFlowSupportService.firstNonBlank(
            queryParams.get("tran_id"),
            queryParams.get("payment_id"),
            queryParams.get("paymentID"));
    if (providerEventId == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Missing required callback transaction identifier");
    }
    Order order = paymentFlowSupportService.requireOrder(orderId);
    paymentFlowSupportService.validateProviderTransactionId(order, providerEventId);
    if (order.getProvider() == OrderProvider.SSLCOMMERZ
        && !"true".equalsIgnoreCase(queryParams.get("_verified_webhook"))) {
      paymentFlowSupportService.recordCallbackEvent(
          order, PaymentEventType.CALLBACK_FAILED, queryParams, PaymentEventStatus.RECEIVED);
      return;
    }
    paymentFlowSupportService.recordCallbackEvent(
        order, PaymentEventType.CALLBACK_FAILED, queryParams, PaymentEventStatus.PROCESSED);
    paymentFlowSupportService.transitionFailed(order);
  }
}

package com.gii.api.service.payment.webhook;

import com.gii.api.model.response.payment.PaymentStatusResponse;
import com.gii.api.service.payment.PaymentFlowSupportService;
import com.gii.common.entity.order.Order;
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
public class PaymentSuccessWebhookService {

  private final PaymentFlowSupportService flowSupportService;

  public PaymentStatusResponse execute(UUID orderId, Map<String, String> params) {
    String providerEventId =
        flowSupportService.firstNonBlank(
            params.get("tran_id"), params.get("payment_id"), params.get("paymentID"));
    if (providerEventId == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Missing required webhook transaction identifier");
    }
    Order order = flowSupportService.requireOrder(orderId);
    flowSupportService.validateProviderTransactionId(order, providerEventId);
    flowSupportService.recordCallbackEvent(
        order, PaymentEventType.WEBHOOK_SUCCESS, params, PaymentEventStatus.PROCESSED);
    PaymentStatusResponse response = flowSupportService.markPaidAndBuildResponse(order);
    flowSupportService.grantEnrollmentsForPaidOrder(order.getId());
    return response;
  }
}

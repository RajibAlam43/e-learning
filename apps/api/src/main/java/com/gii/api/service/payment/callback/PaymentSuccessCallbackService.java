package com.gii.api.service.payment.callback;

import com.gii.api.model.response.payment.PaymentStatusResponse;
import com.gii.api.service.payment.PaymentFlowSupportService;
import com.gii.api.service.payment.bkash.BkashCheckoutService;
import com.gii.api.service.payment.sslcommerz.SslcommerzCallbackValidationService;
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
public class PaymentSuccessCallbackService {

  private final PaymentFlowSupportService flowSupportService;
  private final BkashCheckoutService bkashCheckoutService;
  private final SslcommerzCallbackValidationService sslcommerzCallbackValidationService;

  public PaymentStatusResponse execute(UUID orderId, Map<String, String> queryParams) {
    String providerEventId =
        flowSupportService.firstNonBlank(
            queryParams.get("tran_id"),
            queryParams.get("payment_id"),
            queryParams.get("paymentID"));
    if (providerEventId == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Missing required callback transaction identifier");
    }
    Order order = flowSupportService.requireOrder(orderId);
    flowSupportService.validateProviderTransactionId(order, providerEventId);

    if (order.getProvider() == OrderProvider.SSLCOMMERZ) {
      sslcommerzCallbackValidationService.validateSuccessCallback(order, queryParams);
      flowSupportService.recordCallbackEvent(
          order, PaymentEventType.CALLBACK_SUCCESS_REDIRECT, queryParams, PaymentEventStatus.PROCESSED);
    }

    if (order.getProvider() == OrderProvider.BKASH) {
      bkashCheckoutService.validateSuccessCallback(order, queryParams);
      flowSupportService.recordCallbackEvent(
              order, PaymentEventType.CALLBACK_SUCCESS, queryParams, PaymentEventStatus.PROCESSED);
    }

    PaymentStatusResponse response = flowSupportService.markPaidAndBuildResponse(order);
    flowSupportService.grantEnrollmentsForPaidOrder(order.getId());
    return response;
  }
}

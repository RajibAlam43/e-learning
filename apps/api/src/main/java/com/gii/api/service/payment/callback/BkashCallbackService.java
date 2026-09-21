package com.gii.api.service.payment.callback;

import com.gii.api.service.payment.PaymentFlowSupportService;
import com.gii.api.service.payment.bkash.BkashCheckoutService;
import com.gii.common.entity.order.Order;
import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.PaymentEventStatus;
import com.gii.common.enums.PaymentEventType;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class BkashCallbackService {

  private final PaymentFlowSupportService flowSupportService;
  private final BkashCheckoutService bkashCheckoutService;

  public void successRedirect(UUID orderId, Map<String, String> queryParams) {
    String providerEventId =
        flowSupportService.firstNonBlank(
            queryParams.get("payment_id"),
            queryParams.get("paymentID"),
            queryParams.get("tran_id"));
    if (providerEventId == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Missing required callback transaction identifier");
    }
    Order order = flowSupportService.requireOrder(orderId);
    flowSupportService.validateProviderTransactionId(order, OrderProvider.BKASH, providerEventId);
    bkashCheckoutService.validateSuccessCallback(order, queryParams);
    flowSupportService.recordCallbackEvent(
        order, PaymentEventType.CALLBACK_SUCCESS, queryParams, PaymentEventStatus.PROCESSED);
    flowSupportService.markPaidAndGrant(order);
  }

  public void failedRedirect(UUID orderId, Map<String, String> queryParams) {
    String providerEventId =
        flowSupportService.firstNonBlank(
            queryParams.get("payment_id"),
            queryParams.get("paymentID"),
            queryParams.get("tran_id"));
    if (providerEventId == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Missing required callback transaction identifier");
    }
    Order order = flowSupportService.requireOrder(orderId);
    boolean currentAttempt =
        flowSupportService.validateTerminalProviderTransactionId(
            order, OrderProvider.BKASH, providerEventId);
    flowSupportService.recordCallbackEvent(
        order,
        OrderProvider.BKASH,
        PaymentEventType.CALLBACK_FAILED,
        queryParams,
        PaymentEventStatus.PROCESSED);
    if (currentAttempt) {
      flowSupportService.transitionFailed(order);
    }
  }

  public void cancelledRedirect(UUID orderId, Map<String, String> queryParams) {
    String providerEventId =
        flowSupportService.firstNonBlank(
            queryParams.get("payment_id"),
            queryParams.get("paymentID"),
            queryParams.get("tran_id"));
    if (providerEventId == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Missing required callback transaction identifier");
    }
    Order order = flowSupportService.requireOrder(orderId);
    boolean currentAttempt =
        flowSupportService.validateTerminalProviderTransactionId(
            order, OrderProvider.BKASH, providerEventId);
    flowSupportService.recordCallbackEvent(
        order,
        OrderProvider.BKASH,
        PaymentEventType.CALLBACK_CANCELLED,
        queryParams,
        PaymentEventStatus.PROCESSED);
    if (currentAttempt) {
      flowSupportService.transitionCancelled(order);
    }
  }

  public void successFromWebhook(UUID orderId, Map<String, String> params) {
    String providerEventId =
        flowSupportService.firstNonBlank(
            params.get("payment_id"), params.get("paymentID"), params.get("tran_id"));
    if (providerEventId == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Missing required webhook transaction identifier");
    }
    Order order = flowSupportService.requireOrder(orderId);
    flowSupportService.validateProviderTransactionId(order, OrderProvider.BKASH, providerEventId);
    flowSupportService.recordCallbackEvent(
        order, PaymentEventType.WEBHOOK_SUCCESS, params, PaymentEventStatus.PROCESSED);
    flowSupportService.markPaidAndGrant(order);
  }

  public void failedFromWebhook(UUID orderId, Map<String, String> params) {
    failedRedirect(orderId, params);
  }

  public void cancelledFromWebhook(UUID orderId, Map<String, String> params) {
    cancelledRedirect(orderId, params);
  }
}

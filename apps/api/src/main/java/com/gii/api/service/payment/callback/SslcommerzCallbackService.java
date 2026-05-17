package com.gii.api.service.payment.callback;

import com.gii.api.service.payment.PaymentFlowSupportService;
import com.gii.api.service.payment.sslcommerz.SslcommerzCallbackValidationService;
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
public class SslcommerzCallbackService {

  private final PaymentFlowSupportService flowSupportService;
  private final SslcommerzCallbackValidationService sslcommerzCallbackValidationService;

  public void successRedirect(UUID orderId, Map<String, String> queryParams) {
    String providerEventId = flowSupportService.firstNonBlank(queryParams.get("tran_id"));
    if (providerEventId == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Missing required callback transaction identifier");
    }
    Order order = flowSupportService.requireOrder(orderId);
    flowSupportService.validateProviderTransactionId(order, providerEventId);
    sslcommerzCallbackValidationService.validateSuccessCallback(order, queryParams);
    flowSupportService.recordCallbackEvent(
        order, PaymentEventType.CALLBACK_SUCCESS_REDIRECT, queryParams, PaymentEventStatus.PROCESSED);
  }

  public void failedRedirect(UUID orderId, Map<String, String> queryParams) {
    String providerEventId = flowSupportService.firstNonBlank(queryParams.get("tran_id"));
    if (providerEventId == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Missing required callback transaction identifier");
    }
    Order order = flowSupportService.requireOrder(orderId);
    flowSupportService.validateProviderTransactionId(order, providerEventId);
    flowSupportService.recordCallbackEvent(
        order, PaymentEventType.CALLBACK_FAILED, queryParams, PaymentEventStatus.RECEIVED);
  }

  public void cancelledRedirect(UUID orderId, Map<String, String> queryParams) {
    String providerEventId = flowSupportService.firstNonBlank(queryParams.get("tran_id"));
    if (providerEventId == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Missing required callback transaction identifier");
    }
    Order order = flowSupportService.requireOrder(orderId);
    flowSupportService.validateProviderTransactionId(order, providerEventId);
    flowSupportService.recordCallbackEvent(
        order, PaymentEventType.CALLBACK_CANCELLED, queryParams, PaymentEventStatus.RECEIVED);
  }

  public void successFromWebhook(UUID orderId, Map<String, String> params) {
    String providerEventId = flowSupportService.firstNonBlank(params.get("tran_id"));
    if (providerEventId == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Missing required webhook transaction identifier");
    }
    Order order = flowSupportService.requireOrder(orderId);
    flowSupportService.validateProviderTransactionId(order, providerEventId);
    flowSupportService.recordCallbackEvent(
        order, PaymentEventType.WEBHOOK_SUCCESS, params, PaymentEventStatus.PROCESSED);
    flowSupportService.markPaid(order);
    flowSupportService.grantEnrollmentsForPaidOrder(order.getId());
  }

  public void failedFromWebhook(UUID orderId, Map<String, String> queryParams) {
    String providerEventId = flowSupportService.firstNonBlank(queryParams.get("tran_id"));
    if (providerEventId == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Missing required callback transaction identifier");
    }
    Order order = flowSupportService.requireOrder(orderId);
    flowSupportService.validateProviderTransactionId(order, providerEventId);
    flowSupportService.recordCallbackEvent(
        order, PaymentEventType.CALLBACK_FAILED, queryParams, PaymentEventStatus.PROCESSED);
    flowSupportService.transitionFailed(order);
  }

  public void cancelledFromWebhook(UUID orderId, Map<String, String> queryParams) {
    String providerEventId = flowSupportService.firstNonBlank(queryParams.get("tran_id"));
    if (providerEventId == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Missing required callback transaction identifier");
    }
    Order order = flowSupportService.requireOrder(orderId);
    flowSupportService.validateProviderTransactionId(order, providerEventId);
    flowSupportService.recordCallbackEvent(
        order, PaymentEventType.CALLBACK_CANCELLED, queryParams, PaymentEventStatus.PROCESSED);
    flowSupportService.transitionCancelled(order);
  }
}

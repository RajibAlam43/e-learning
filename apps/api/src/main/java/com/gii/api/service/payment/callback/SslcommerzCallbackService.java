package com.gii.api.service.payment.callback;

import com.gii.api.service.payment.PaymentFlowSupportService;
import com.gii.api.service.payment.sslcommerz.SslcommerzCallbackValidationService;
import com.gii.common.entity.order.Order;
import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.PaymentEventStatus;
import com.gii.common.enums.PaymentEventType;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
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
    flowSupportService.validateProviderTransactionId(
        order, OrderProvider.SSLCOMMERZ, providerEventId);
    try {
      sslcommerzCallbackValidationService.validateSuccessCallback(order, queryParams);
    } catch (ResponseStatusException ex) {
      log.warn(
          "SSLCommerz success callback validation skipped; orderId={}, tran_id={}, reason={}",
          orderId,
          providerEventId,
          ex.getReason());
    }
    flowSupportService.recordCallbackEvent(
        order,
        PaymentEventType.CALLBACK_SUCCESS_REDIRECT,
        queryParams,
        PaymentEventStatus.PROCESSED);
  }

  public void failedRedirect(UUID orderId, Map<String, String> queryParams) {
    String providerEventId = flowSupportService.firstNonBlank(queryParams.get("tran_id"));
    if (providerEventId == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Missing required callback transaction identifier");
    }
    Order order = flowSupportService.requireOrder(orderId);
    flowSupportService.validateTerminalProviderTransactionId(
        order, OrderProvider.SSLCOMMERZ, providerEventId);
    flowSupportService.recordCallbackEvent(
        order,
        OrderProvider.SSLCOMMERZ,
        PaymentEventType.CALLBACK_FAILED,
        queryParams,
        PaymentEventStatus.RECEIVED);
  }

  public void cancelledRedirect(UUID orderId, Map<String, String> queryParams) {
    String providerEventId = flowSupportService.firstNonBlank(queryParams.get("tran_id"));
    if (providerEventId == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Missing required callback transaction identifier");
    }
    Order order = flowSupportService.requireOrder(orderId);
    flowSupportService.validateTerminalProviderTransactionId(
        order, OrderProvider.SSLCOMMERZ, providerEventId);
    flowSupportService.recordCallbackEvent(
        order,
        OrderProvider.SSLCOMMERZ,
        PaymentEventType.CALLBACK_CANCELLED,
        queryParams,
        PaymentEventStatus.RECEIVED);
  }

  public void successFromWebhook(UUID orderId, Map<String, String> params) {
    String providerEventId = flowSupportService.firstNonBlank(params.get("tran_id"));
    if (providerEventId == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Missing required webhook transaction identifier");
    }
    Order order = flowSupportService.requireOrder(orderId);
    flowSupportService.validateProviderTransactionId(
        order, OrderProvider.SSLCOMMERZ, providerEventId);
    flowSupportService.recordCallbackEvent(
        order, PaymentEventType.WEBHOOK_SUCCESS, params, PaymentEventStatus.PROCESSED);
    flowSupportService.markPaidAndGrant(order);
  }

  public void failedFromWebhook(UUID orderId, Map<String, String> queryParams) {
    String providerEventId = flowSupportService.firstNonBlank(queryParams.get("tran_id"));
    if (providerEventId == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Missing required callback transaction identifier");
    }
    Order order = flowSupportService.requireOrder(orderId);
    boolean currentAttempt =
        flowSupportService.validateTerminalProviderTransactionId(
            order, OrderProvider.SSLCOMMERZ, providerEventId);
    flowSupportService.recordCallbackEvent(
        order,
        OrderProvider.SSLCOMMERZ,
        PaymentEventType.CALLBACK_FAILED,
        queryParams,
        PaymentEventStatus.PROCESSED);
    if (currentAttempt) {
      flowSupportService.transitionFailed(order);
    }
  }

  public void cancelledFromWebhook(UUID orderId, Map<String, String> queryParams) {
    String providerEventId = flowSupportService.firstNonBlank(queryParams.get("tran_id"));
    if (providerEventId == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Missing required callback transaction identifier");
    }
    Order order = flowSupportService.requireOrder(orderId);
    boolean currentAttempt =
        flowSupportService.validateTerminalProviderTransactionId(
            order, OrderProvider.SSLCOMMERZ, providerEventId);
    flowSupportService.recordCallbackEvent(
        order,
        OrderProvider.SSLCOMMERZ,
        PaymentEventType.CALLBACK_CANCELLED,
        queryParams,
        PaymentEventStatus.PROCESSED);
    if (currentAttempt) {
      flowSupportService.transitionCancelled(order);
    }
  }
}

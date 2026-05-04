package com.gii.api.service.payment;

import com.gii.api.model.response.payment.WebhookAckResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentWebhookService {

  private final SslcommerzWebhookService sslcommerzWebhookService;
  private final BkashWebhookService bkashWebhookService;

  public WebhookAckResponse sslcommerz(Map<String, String> headers, String payload) {
    return sslcommerzWebhookService.handle(headers, payload);
  }

  public WebhookAckResponse bkash(Map<String, String> headers, String payload) {
    return bkashWebhookService.handle(headers, payload);
  }
}

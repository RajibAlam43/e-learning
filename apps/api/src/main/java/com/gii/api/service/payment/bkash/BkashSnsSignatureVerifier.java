package com.gii.api.service.payment.bkash;

import org.springframework.stereotype.Component;

@Component
public class BkashSnsSignatureVerifier {

  public boolean isValid(Message message) {
    if (!"1".equals(message.SignatureVersion())) {
      return false;
    }
    return WebhookUtility.isMessageSignatureValid(message);
  }
}

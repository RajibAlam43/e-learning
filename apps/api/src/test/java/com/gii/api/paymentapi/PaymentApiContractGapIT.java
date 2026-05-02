package com.gii.api.paymentapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class PaymentApiContractGapIt extends AbstractPaymentApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupPaymentData();
  }

  @Test
  void paymentProviderCallbacksShouldBePublicWithoutRoleAuthentication() throws Exception {
    mockMvc
        .perform(
            get("/payments/{orderId}/success", UUID.randomUUID())
                .param("tran_id", "public-callback"))
        .andExpect(status().isNotFound());
  }

  @Test
  void paymentWebhooksShouldBePublicWithoutRoleAuthentication() throws Exception {
    mockMvc
        .perform(
            post("/public/webhooks/payments/sslcommerz")
                .contentType(MediaType.TEXT_PLAIN)
                .header("x-signature", "missing-secret-will-fail-validation-not-auth")
                .content("{\"event\":\"payment\"}"))
        .andExpect(status().isBadRequest());
  }
}

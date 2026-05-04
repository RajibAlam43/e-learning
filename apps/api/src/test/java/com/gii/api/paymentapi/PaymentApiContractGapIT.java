package com.gii.api.paymentapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
                .content(signedSslPayload("tran_id=public-callback&status=UNATTEMPTED&val_id=val-public")))
        .andExpect(status().isOk());
  }

  private String signedSslPayload(String basePayload) {
    String verifyKey = "status,tran_id,val_id";
    String signSource = signSource(basePayload, verifyKey);
    String verifySign = md5Hex(signSource + "&store_passwd=" + md5Hex("test-password")).toUpperCase();
    return basePayload + "&verify_key=" + verifyKey + "&verify_sign=" + verifySign;
  }

  private String signSource(String payload, String verifyKey) {
    List<String> fragments = new ArrayList<>();
    String[] pairs = payload.split("&");
    for (String key : verifyKey.split(",")) {
      for (String pair : pairs) {
        if (pair.startsWith(key + "=")) {
          fragments.add(pair);
          break;
        }
      }
    }
    fragments.sort(Comparator.naturalOrder());
    return String.join("&", fragments);
  }

  private String md5Hex(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(digest.length * 2);
      for (byte b : digest) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}

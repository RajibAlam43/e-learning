package com.gii.api.service.payment.sslcommerz;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gii.common.entity.order.Order;
import com.gii.common.entity.order.OrderItem;
import com.gii.common.repository.order.OrderItemRepository;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SslcommerzCheckoutService {

  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final ObjectMapper objectMapper;
  private final WebClient.Builder webClientBuilder;
  private final OrderItemRepository orderItemRepository;

  @Value("${payments.sslcommerz.session-api-url}")
  private String sessionApiUrl;

  @Value("${payments.sslcommerz.store-id}")
  private String storeId;

  @Value("${payments.sslcommerz.store-password}")
  private String storePassword;

  @Value("${payments.sslcommerz.request-timeout-ms}")
  private long timeoutMs;

  @Value("${payments.callback-base-url}")
  private String callbackBaseUrl;

  public boolean isConfigured() {
    return !isBlank(sessionApiUrl) && !isBlank(storeId) && !isBlank(storePassword) && !isBlank(callbackBaseUrl);
  }

  public InitiationResult createSession(Order order, String customerName, String customerEmail, String customerPhone) {
    if (!isConfigured()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SSLCommerz is not configured");
    }
    if (isBlank(customerEmail) || isBlank(customerPhone)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer contact info is required");
    }

    String tranId = buildTranId(order.getId().toString());
    String productName = resolveProductName(order.getId());

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("store_id", storeId);
    form.add("store_passwd", storePassword);
    form.add("total_amount", order.getAmountBdt().toPlainString());
    form.add("currency", order.getCurrency());
    form.add("tran_id", tranId);
    form.add("success_url", callbackBaseUrl + "/payments/" + order.getId() + "/success");
    form.add("fail_url", callbackBaseUrl + "/payments/" + order.getId() + "/failed");
    form.add("cancel_url", callbackBaseUrl + "/payments/" + order.getId() + "/cancelled");
    form.add("ipn_url", callbackBaseUrl + "/public/webhooks/payments/sslcommerz");
    form.add("cus_name", customerName == null || customerName.isBlank() ? "Student" : customerName);
    form.add("cus_email", customerEmail);
    form.add("cus_phone", customerPhone);
    form.add("product_name", productName);
    form.add("product_category", "course");
    form.add("product_profile", "non-physical-goods");
    form.add("shipping_method", "NO");

    Map<String, Object> response = requestSession(form);
    String status = asString(response.get("status"));
    if (!"SUCCESS".equalsIgnoreCase(status)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SSLCommerz session creation failed");
    }

    String gatewayPageUrl = asString(response.get("GatewayPageURL"));
    String sessionKey = asString(response.get("sessionkey"));
    if (isBlank(gatewayPageUrl) || isBlank(sessionKey)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SSLCommerz session creation failed");
    }
    return new InitiationResult(tranId, sessionKey, gatewayPageUrl);
  }

  private Map<String, Object> requestSession(MultiValueMap<String, String> form) {
    try {
      String body =
          webClientBuilder
              .build()
              .post()
              .uri(sessionApiUrl)
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .accept(MediaType.APPLICATION_JSON)
              .body(BodyInserters.fromFormData(form))
              .retrieve()
              .bodyToMono(String.class)
              .block(Duration.ofMillis(timeoutMs));
      if (body == null || body.isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SSLCommerz session creation failed");
      }
      return objectMapper.readValue(body, MAP_TYPE);
    } catch (Exception ex) {
      if (ex instanceof ResponseStatusException rse) {
        throw rse;
      }
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SSLCommerz session creation failed", ex);
    }
  }

  private String resolveProductName(java.util.UUID orderId) {
    List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
    if (items.isEmpty()) {
      return "Collection";
    }
    if (items.size() == 1) {
      return items.getFirst().getTitleSnapshot();
    }
    return items.getFirst().getTitleSnapshot() + " + " + (items.size() - 1) + " more";
  }

  private String buildTranId(String orderId) {
    String normalized = orderId.replace("-", "");
    return normalized.length() <= 30 ? normalized : normalized.substring(0, 30);
  }

  private String asString(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  public record InitiationResult(String tranId, String sessionKey, String gatewayPageUrl) {}
}

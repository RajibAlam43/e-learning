package com.gii.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

@Component
@Slf4j
public class SslcommerzRequestLoggingFilter extends OncePerRequestFilter {

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String uri = request.getRequestURI();
    return !(uri.startsWith("/payments/sslcommerz/")
        || uri.startsWith("/public/webhooks/payments/sslcommerz"));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    ContentCachingRequestWrapper wrappedRequest =
        new ContentCachingRequestWrapper(request, 64 * 1024);
    try {
      filterChain.doFilter(wrappedRequest, response);
    } finally {
      byte[] body = wrappedRequest.getContentAsByteArray();
      String rawBody = body.length == 0 ? "" : new String(body, StandardCharsets.UTF_8);
      Map<String, String[]> parameterMap =
          wrappedRequest.getParameterMap() == null
              ? Collections.emptyMap()
              : wrappedRequest.getParameterMap();
      String params =
          parameterMap.entrySet().stream()
              .map(
                  entry ->
                      entry.getKey()
                          + "="
                          + String.join(",", entry.getValue() == null ? new String[] {} : entry.getValue()))
              .collect(Collectors.joining("&"));
      log.info(
          "SSLCommerz raw request: method={}, uri={}, query={}, params={}, body={}, status={}",
          wrappedRequest.getMethod(),
          wrappedRequest.getRequestURI(),
          wrappedRequest.getQueryString(),
          params,
          rawBody,
          response.getStatus());
    }
  }
}

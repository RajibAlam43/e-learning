package com.gii.api.service.pub;

import com.gii.api.model.request.CreateSupportTicketRequest;
import com.gii.api.model.response.SupportTicketCreatedResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.entity.support.SupportTicket;
import com.gii.common.entity.user.User;
import com.gii.common.repository.support.SupportTicketRepository;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class SupportTicketService {

  private static final int MAX_NAME_LEN = 150;
  private static final int MAX_EMAIL_LEN = 255;
  private static final int MAX_PHONE_LEN = 30;
  private static final int MAX_SUBJECT_LEN = 200;
  private static final int MAX_MESSAGE_LEN = 10_000;

  private final SupportTicketRepository supportTicketRepository;
  private final CurrentUserService currentUserService;

  @Value("${support.ticket.rate-limit-seconds:60}")
  private long rateLimitSeconds;

  public SupportTicketCreatedResponse execute(
      CreateSupportTicketRequest request, Authentication authentication) {
    if (request == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request");
    }

    String name = normalize(request.name(), MAX_NAME_LEN);
    String email = normalize(request.email(), MAX_EMAIL_LEN);
    String phone = normalize(request.phone(), MAX_PHONE_LEN);
    final String subject =
        normalizeRequired(request.subject(), MAX_SUBJECT_LEN, "Subject is required");
    final String message =
        normalizeRequired(request.message(), MAX_MESSAGE_LEN, "Message is required");

    User user = null;
    if (authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken)) {
      user = currentUserService.getCurrentUser(authentication);
      if (name == null) {
        name = user.getFullName();
      }
      if (email == null) {
        email = user.getEmail();
      }
      if (phone == null) {
        phone = user.getPhone();
      }
    }

    if (email == null && phone == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Either email or phone is required");
    }

    Instant threshold = Instant.now().minus(Duration.ofSeconds(rateLimitSeconds));
    boolean recentlySubmitted =
        (email != null && supportTicketRepository.existsByEmailAndCreatedAtAfter(email, threshold))
            || (phone != null
                && supportTicketRepository.existsByPhoneAndCreatedAtAfter(phone, threshold));
    if (recentlySubmitted) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
    }

    SupportTicket ticket =
        SupportTicket.builder()
            .user(user)
            .name(name)
            .email(email)
            .phone(phone)
            .subject(subject)
            .message(message)
            .build();

    SupportTicket saved = supportTicketRepository.save(ticket);

    return SupportTicketCreatedResponse.builder()
        .ticketId(saved.getId())
        .status(saved.getStatus().name())
        .createdAt(saved.getCreatedAt())
        .build();
  }

  private String normalize(String value, int maxLen) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    if (trimmed.length() > maxLen) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Field exceeds allowed length");
    }
    return trimmed;
  }

  private String normalizeRequired(String value, int maxLen, String requiredMessage) {
    String normalized = normalize(value, maxLen);
    if (normalized == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, requiredMessage);
    }
    return normalized;
  }
}

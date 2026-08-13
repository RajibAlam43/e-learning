package com.gii.api.service.pub;

import com.gii.api.model.request.CreateSupportTicketRequest;
import com.gii.api.model.response.SupportTicketCreatedResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.api.service.util.IdentifierNormalizationUtil;
import com.gii.common.entity.support.SupportTicket;
import com.gii.common.entity.user.User;
import com.gii.common.enums.VerificationChannel;
import com.gii.common.repository.support.SupportTicketRepository;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
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
      CreateSupportTicketRequest request, Authentication authentication, String remoteAddress) {
    if (request == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request");
    }

    String name = normalize(request.name(), MAX_NAME_LEN);
    String email = normalizeEmail(request.email());
    String phone = normalizePhone(request.phone());
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
        email = normalizeEmail(user.getEmail());
      }
      if (phone == null) {
        phone = normalizePhone(user.getPhone());
      }
    }

    if (email == null && phone == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Either email or phone is required");
    }

    RateLimitIdentity rateLimitIdentity = rateLimitIdentity(user, remoteAddress);
    String rateLimitKeyHash = rateLimitIdentity.hash();
    supportTicketRepository.lockRateLimitKey(rateLimitIdentity.lockKey());
    Instant threshold = Instant.now().minus(Duration.ofSeconds(rateLimitSeconds));
    boolean recentlySubmitted =
        supportTicketRepository.existsByRateLimitKeyHashAndCreatedAtAfter(
            rateLimitKeyHash, threshold);
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
            .rateLimitKeyHash(rateLimitKeyHash)
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

  private String normalizeEmail(String value) {
    String normalized = normalize(value, MAX_EMAIL_LEN);
    return normalized == null
        ? null
        : IdentifierNormalizationUtil.normalizeIdentifier(VerificationChannel.EMAIL, normalized);
  }

  private String normalizePhone(String value) {
    String normalized = normalize(value, MAX_PHONE_LEN);
    if (normalized == null) {
      return null;
    }
    try {
      return IdentifierNormalizationUtil.normalizeIdentifier(VerificationChannel.PHONE, normalized);
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid phone number", exception);
    }
  }

  private RateLimitIdentity rateLimitIdentity(User user, String remoteAddress) {
    String identity =
        user != null
            ? "user:" + user.getId()
            : "ip:" + normalizeRequired(remoteAddress, 100, "Client identity is required");
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8));
      return new RateLimitIdentity(
          HexFormat.of().formatHex(digest), ByteBuffer.wrap(digest).getLong());
    } catch (java.security.NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  private record RateLimitIdentity(String hash, long lockKey) {}
}

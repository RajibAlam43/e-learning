package com.gii.api.service.pub;

import com.gii.api.model.request.CreateSupportTicketRequest;
import com.gii.common.entity.support.SupportTicket;
import com.gii.common.repository.support.SupportTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

  public void execute(CreateSupportTicketRequest request) {
    if (request == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request");
    }

    String name = normalize(request.name(), MAX_NAME_LEN);
    String email = normalize(request.email(), MAX_EMAIL_LEN);
    String phone = normalize(request.phone(), MAX_PHONE_LEN);
    String subject = normalizeRequired(request.subject(), MAX_SUBJECT_LEN, "Subject is required");
    String message = normalizeRequired(request.message(), MAX_MESSAGE_LEN, "Message is required");

    if (email == null && phone == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Either email or phone is required");
    }

    SupportTicket ticket =
        SupportTicket.builder()
            .name(name)
            .email(email)
            .phone(phone)
            .subject(subject)
            .message(message)
            .build();

    supportTicketRepository.save(ticket);

    // Placeholder: enqueue async notification job (email/slack/crm) after persistence.
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

package com.gii.api.service.admin;

import com.gii.api.model.request.admin.UpdateSupportTicketRequest;
import com.gii.api.model.response.admin.AdminSupportTicketResponse;
import com.gii.common.entity.support.SupportTicket;
import com.gii.common.enums.SupportTicketStatus;
import com.gii.common.repository.support.SupportTicketRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminSupportTicketManagementService {

  private final SupportTicketRepository supportTicketRepository;

  @Transactional(readOnly = true)
  public List<AdminSupportTicketResponse> list(SupportTicketStatus status) {
    List<SupportTicket> tickets =
        status == null
            ? supportTicketRepository.findAll()
            : supportTicketRepository.findByStatusOrderByCreatedAtDesc(status);
    return tickets.stream()
        .sorted(Comparator.comparing(SupportTicket::getCreatedAt).reversed())
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public AdminSupportTicketResponse get(UUID ticketId) {
    return toResponse(find(ticketId));
  }

  public AdminSupportTicketResponse update(UUID ticketId, UpdateSupportTicketRequest request) {
    SupportTicket ticket = find(ticketId);
    ticket.setStatus(request.status());
    ticket.setClosedAt(request.status() == SupportTicketStatus.CLOSED ? Instant.now() : null);
    return toResponse(supportTicketRepository.save(ticket));
  }

  private SupportTicket find(UUID ticketId) {
    return supportTicketRepository
        .findById(ticketId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Support ticket not found"));
  }

  private AdminSupportTicketResponse toResponse(SupportTicket ticket) {
    return AdminSupportTicketResponse.builder()
        .ticketId(ticket.getId())
        .userId(ticket.getUser() != null ? ticket.getUser().getId() : null)
        .name(ticket.getName())
        .email(ticket.getEmail())
        .phone(ticket.getPhone())
        .subject(ticket.getSubject())
        .message(ticket.getMessage())
        .status(ticket.getStatus())
        .closedAt(ticket.getClosedAt())
        .createdAt(ticket.getCreatedAt())
        .updatedAt(ticket.getUpdatedAt())
        .build();
  }
}

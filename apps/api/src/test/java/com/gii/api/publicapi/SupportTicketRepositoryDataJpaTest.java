package com.gii.api.publicapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.gii.common.entity.support.SupportTicket;
import org.junit.jupiter.api.Test;

class SupportTicketRepositoryDataJpaTest extends AbstractPublicDataJpaTest {

  @Test
  void persistsTicketWithNullableContactFields() {
    SupportTicket ticket =
        supportTicketRepository.save(
            SupportTicket.builder()
                .name("User Name")
                .phone("01700000000")
                .subject("Support subject")
                .message("Support message")
                .build());

    var found = supportTicketRepository.findById(ticket.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getEmail()).isNull();
    assertThat(found.get().getPhone()).isEqualTo("01700000000");
  }

  @Test
  void persistsTicketWithEmailOnly() {
    SupportTicket ticket =
        supportTicketRepository.save(
            SupportTicket.builder()
                .name("User Name")
                .email("user@example.com")
                .subject("Support subject")
                .message("Support message")
                .build());

    var found = supportTicketRepository.findById(ticket.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getEmail()).isEqualTo("user@example.com");
    assertThat(found.get().getPhone()).isNull();
  }
}

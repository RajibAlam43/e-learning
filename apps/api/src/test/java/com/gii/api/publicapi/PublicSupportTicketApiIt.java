package com.gii.api.publicapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class PublicSupportTicketApiIt extends AbstractPublicApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void createsSupportTicketAndPersistsNormalizedValues() throws Exception {
    String body =
        """
        {
          "name": "  Rajib Alam  ",
          "email": "rajib@example.com",
          "subject": "  Need help with enrollment  ",
          "message": "  Please assist me.  "
        }
        """;

    mockMvc
        .perform(post("/public/support/tickets").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isOk());

    assertThat(supportTicketCount()).isEqualTo(1);
    var ticket = latestSupportTicket();
    assertThat(ticket.getName()).isEqualTo("Rajib Alam");
    assertThat(ticket.getEmail()).isEqualTo("rajib@example.com");
    assertThat(ticket.getSubject()).isEqualTo("Need help with enrollment");
    assertThat(ticket.getMessage()).isEqualTo("Please assist me.");
  }

  @Test
  void rejectsTicketWhenBothEmailAndPhoneMissing() throws Exception {
    String body =
        """
        {
          "subject": "Support",
          "message": "Please call me"
        }
        """;

    mockMvc
        .perform(post("/public/support/tickets").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());

    assertThat(supportTicketCount()).isZero();
  }

  @Test
  void createsTicketWithPhoneOnly() throws Exception {
    String body =
        """
        {
          "phone": "01700000000",
          "subject": "Phone support",
          "message": "Call me"
        }
        """;

    mockMvc
        .perform(post("/public/support/tickets").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isOk());

    assertThat(supportTicketCount()).isEqualTo(1);
    assertThat(latestSupportTicket().getPhone()).isEqualTo("01700000000");
  }

  @Test
  void rejectsInvalidEmailOrBlankRequiredFields() throws Exception {
    String invalidEmail =
        """
        {
          "email": "bad-email",
          "subject": "Support",
          "message": "Need help"
        }
        """;
    mockMvc
        .perform(
            post("/public/support/tickets").contentType(APPLICATION_JSON).content(invalidEmail))
        .andExpect(status().isBadRequest());

    String blankSubject =
        """
        {
          "email": "x@example.com",
          "subject": "   ",
          "message": "Need help"
        }
        """;
    mockMvc
        .perform(
            post("/public/support/tickets").contentType(APPLICATION_JSON).content(blankSubject))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsOverMaxLengthFields() throws Exception {
    String longSubject = "S".repeat(201);
    String body =
        """
        {
          "email": "x@example.com",
          "subject": "%s",
          "message": "Need help"
        }
        """
            .formatted(longSubject);

    mockMvc
        .perform(post("/public/support/tickets").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());
    assertThat(supportTicketCount()).isZero();
  }
}

package com.gii.api.publicapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.UserStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
        .andExpect(status().isCreated());

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
        .andExpect(status().isCreated());

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

  @Test
  void rateLimitsRepeatedTicketFromSameContact() throws Exception {
    String body =
        """
        {
          "email": "rate-limit@example.com",
          "subject": "First request",
          "message": "Please help"
        }
        """;

    mockMvc
        .perform(post("/public/support/tickets").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.ticketId").isNotEmpty())
        .andExpect(jsonPath("$.status").value("OPEN"));

    mockMvc
        .perform(post("/public/support/tickets").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isTooManyRequests());

    assertThat(supportTicketCount()).isEqualTo(1);
  }

  @Test
  void authenticatedTicketAssociatesUserAndFillsMissingContact() throws Exception {
    var student = user("Signed In Student", "signed-in-support@example.com", UserStatus.ACTIVE);
    var auth =
        new UsernamePasswordAuthenticationToken(
            student.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

    mockMvc
        .perform(
            post("/public/support/tickets")
                .with(authentication(auth))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "subject":"Account help",
                      "message":"Please help with my account"
                    }
                    """))
        .andExpect(status().isCreated());

    var ticket = latestSupportTicket();
    assertThat(ticket.getUser().getId()).isEqualTo(student.getId());
    assertThat(ticket.getName()).isEqualTo("Signed In Student");
    assertThat(ticket.getEmail()).isEqualTo("signed-in-support@example.com");
  }
}

package com.gii.worker.service;

import com.gii.common.dto.EmailJobMessage;
import com.gii.common.enums.VerificationPurpose;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class VerificationEmailTemplateService {
  private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([A-Z_]+)}}");
  private static final DateTimeFormatter EXPIRY =
      DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm 'UTC'").withZone(ZoneOffset.UTC);
  private final String template;

  public VerificationEmailTemplateService() {
    try {
      template =
          new ClassPathResource("email/verification.html")
              .getContentAsString(StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new UncheckedIOException("Cannot load verification email template", exception);
    }
  }

  public String render(EmailJobMessage job) {
    boolean reset = job.verificationPurpose() == VerificationPurpose.PASSWORD_RESET;
    String title = reset ? "Reset your password" : "Verify your account";
    String instruction =
        reset
            ? "Use the code below to reset your Global Islamic Institute password."
            : "Use the code below to verify your Global Islamic Institute account.";
    String code = job.verificationCode();
    boolean hasCode = code != null && code.matches("[0-9]{6}");
    String panel =
        hasCode
            ? "<p style=\"margin:0 0 18px;font-size:13px;font-weight:bold;"
                + "letter-spacing:2px;color:#1d4ed8\">YOUR 6-DIGIT CODE</p>"
                + "<p style=\"margin:0;font-size:40px;font-weight:bold;"
                + "letter-spacing:8px;color:#1c3873\">"
                + escape(code)
                + "</p>"
            : "<p style=\"margin:0;font-size:16px;line-height:26px;"
                + "color:#334155;overflow-wrap:anywhere\">"
                + escape(job.body())
                + "</p>";
    // Older queued jobs have no expiry metadata; preserve their original expiry wording.
    String expiry =
        job.expiresAt() != null
            ? "This code expires on " + EXPIRY.format(job.expiresAt()) + "."
            : hasCode ? job.body() : "";
    Map<String, String> values =
        Map.of(
            "TITLE", escape(title),
            "INSTRUCTION", escape(instruction),
            "CODE_PANEL", panel,
            "EXPIRY", escape(expiry));
    return PLACEHOLDER
        .matcher(template)
        .replaceAll(match -> Matcher.quoteReplacement(values.get(match.group(1))));
  }

  private static String escape(String value) {
    return HtmlUtils.htmlEscape(value == null ? "" : value, StandardCharsets.UTF_8.name());
  }
}

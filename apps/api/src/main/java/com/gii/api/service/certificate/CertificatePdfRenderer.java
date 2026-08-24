package com.gii.api.service.certificate;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CertificatePdfRenderer {

  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.ENGLISH).withZone(ZoneOffset.UTC);

  public byte[] render(String resourcePath, CertificateTemplateData data) {
    String html = loadTemplate(resourcePath);
    Map<String, String> replacements =
        Map.of(
            "recipient_name", escapeHtml(data.recipientName()),
            "target_title", escapeHtml(data.targetTitle()),
            "completion_date", DATE_FORMAT.format(data.completionDate()),
            "instructor_name", escapeHtml(data.instructorName()),
            "certificate_code", escapeHtml(data.certificateCode()));
    for (Map.Entry<String, String> replacement : replacements.entrySet()) {
      html = html.replace("{{" + replacement.getKey() + "}}", replacement.getValue());
    }

    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      PdfRendererBuilder builder = new PdfRendererBuilder();
      builder.useFastMode();
      builder.withHtmlContent(html, null);
      builder.toStream(output);
      builder.run();
      return output.toByteArray();
    } catch (Exception ex) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Unable to render certificate PDF", ex);
    }
  }

  private String loadTemplate(String resourcePath) {
    try {
      return new ClassPathResource(resourcePath).getContentAsString(StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Certificate template is unavailable", ex);
    }
  }

  private String escapeHtml(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

  public record CertificateTemplateData(
      String recipientName,
      String targetTitle,
      Instant completionDate,
      String instructorName,
      String certificateCode) {}
}

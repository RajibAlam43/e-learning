package com.gii.api.service.certificate;

import static org.assertj.core.api.Assertions.assertThat;

import com.gii.api.service.certificate.CertificatePdfRenderer.CertificateTemplateData;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class CertificatePdfRendererTest {

  private final CertificatePdfRenderer renderer = new CertificatePdfRenderer();

  @Test
  void rendersBundledCourseTemplateAsPdf() throws Exception {
    byte[] pdf =
        renderer.render(
            "default-course-certificate-v1.html",
            new CertificateTemplateData(
                "Student & Learner",
                "Course <One>",
                Instant.parse("2026-08-23T00:00:00Z"),
                "Instructor",
                "GII-CERT-TEST123"));

    assertThat(pdf).isNotEmpty();
    assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    try (PDDocument document = PDDocument.load(pdf)) {
      String text = new PDFTextStripper().getText(document);
      assertThat(text)
          .contains("Student & Learner")
          .contains("Course <One>")
          .contains("August 23, 2026")
          .contains("Instructor");
      assertThat(text.replaceAll("\\s+", "")).contains("GII-CERT-TEST123");
    }
  }

  @Test
  void rendersBundledProgramTemplateAsPdf() {
    byte[] pdf =
        renderer.render(
            "default-program-certificate-v1.html",
            new CertificateTemplateData(
                "Student",
                "Program One",
                Instant.parse("2026-08-23T00:00:00Z"),
                "Global Islamic Institute",
                "GII-CERT-TEST456"));

    assertThat(pdf).isNotEmpty();
  }
}

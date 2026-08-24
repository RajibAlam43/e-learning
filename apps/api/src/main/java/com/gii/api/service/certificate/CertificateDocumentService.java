package com.gii.api.service.certificate;

import com.gii.api.service.certificate.CertificatePdfRenderer.CertificateTemplateData;
import com.gii.api.service.storage.R2ObjectStorageService;
import com.gii.common.entity.certificate.Certificate;
import com.gii.common.entity.certificate.CertificateTemplate;
import com.gii.common.enums.CertificateTargetType;
import com.gii.common.repository.certificate.CertificateTemplateRepository;
import java.time.Instant;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CertificateDocumentService {

  private static final String COURSE_TEMPLATE_NAME = "Default Course Certificate v1";
  private static final String COLLECTION_TEMPLATE_NAME = "Default Program Certificate v1";
  private static final Pattern SAFE_CLASSPATH_TEMPLATE =
      Pattern.compile("[a-zA-Z0-9][a-zA-Z0-9/_-]*\\.html");

  private final CertificateTemplateRepository templateRepository;
  private final CertificatePdfRenderer pdfRenderer;
  private final R2ObjectStorageService objectStorageService;

  public StoredCertificateDocument generateAndUpload(
      Certificate certificate, String instructorName, Instant completionDate) {
    CertificateTemplate template = requireTemplate(certificate.getTargetType());
    String resourcePath = String.valueOf(template.getTemplateJson().get("resourcePath"));
    if (resourcePath.isBlank()
        || "null".equals(resourcePath)
        || resourcePath.contains("..")
        || !SAFE_CLASSPATH_TEMPLATE.matcher(resourcePath).matches()) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Certificate template resource is not configured");
    }

    byte[] pdf =
        pdfRenderer.render(
            resourcePath,
            new CertificateTemplateData(
                certificate.getRecipientName(),
                certificate.getTargetTitle(),
                completionDate,
                instructorName,
                certificate.getCertificateCode()));
    String enrollmentScope;
    if (certificate.getEnrollment() != null) {
      enrollmentScope = "course-enrollments/" + certificate.getEnrollment().getId();
    } else if (certificate.getCollectionEnrollment() != null) {
      enrollmentScope = "collection-enrollments/" + certificate.getCollectionEnrollment().getId();
    } else {
      enrollmentScope = "legacy-certificates/" + certificate.getId();
    }
    String objectKey =
        "certificates/" + certificate.getUser().getId() + "/" + enrollmentScope + ".pdf";
    objectStorageService.put(objectKey, pdf, "application/pdf");
    return new StoredCertificateDocument(template, objectKey, pdf.length);
  }

  private CertificateTemplate requireTemplate(CertificateTargetType targetType) {
    String name =
        targetType == CertificateTargetType.COURSE
            ? COURSE_TEMPLATE_NAME
            : COLLECTION_TEMPLATE_NAME;
    var templates = templateRepository.findByNameAndIsActiveTrue(name);
    if (templates.size() != 1) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          templates.isEmpty()
              ? "Active certificate template not found"
              : "Multiple active certificate templates found");
    }
    return templates.getFirst();
  }

  public record StoredCertificateDocument(
      CertificateTemplate template, String objectKey, long sizeBytes) {}
}

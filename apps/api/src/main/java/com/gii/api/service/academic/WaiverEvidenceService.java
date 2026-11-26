package com.gii.api.service.academic;

import com.gii.api.model.request.admin.WaiverEvidenceUploadRequest;
import com.gii.api.service.storage.R2ObjectStorageService;
import com.gii.api.service.storage.R2PresignedUrlService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class WaiverEvidenceService {
  private final R2PresignedUrlService urls;
  private final R2ObjectStorageService objects;
  private static final Map<String, String> TYPES =
      Map.of("application/pdf", "pdf", "image/jpeg", "jpg", "image/png", "png");

  public record Upload(String objectKey, String uploadUrl, Instant expiresAt) {}

  public Upload upload(UUID userId, WaiverEvidenceUploadRequest request) {
    String ext = TYPES.get(request.contentType());
    if (ext == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use a PDF, JPEG or PNG file");
    }
    String key = "waiver-evidence/" + userId + "/" + UUID.randomUUID() + "." + ext;
    var signed = urls.generateUploadUrl(key, request.contentType(), request.sizeBytes());
    return new Upload(key, signed.uploadUrl(), signed.expiresAt());
  }

  public void validate(UUID userId, String key) {
    if (key == null) {
      return;
    }
    if (!key.matches("waiver-evidence/" + userId + "/[a-f0-9-]{36}\\.(pdf|jpg|png)")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid supporting file");
    }
    var file = objects.head(key);
    if (file.contentLength() == null
        || file.contentLength() < 1
        || file.contentLength() > 10485760
        || !TYPES.containsKey(file.contentType())
        || !key.endsWith("." + TYPES.get(file.contentType()))) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Invalid supporting file type or size");
    }
  }

  public R2PresignedUrlService.PresignedDownload download(String key) {
    String ext = key.substring(key.lastIndexOf('.') + 1);
    String mime =
        TYPES.entrySet().stream()
            .filter(e -> e.getValue().equals(ext))
            .findFirst()
            .orElseThrow()
            .getKey();
    return urls.generateDownloadUrl(key, "supporting-file." + ext, mime);
  }
}

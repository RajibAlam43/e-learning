package com.gii.api.service.admin;

import com.gii.api.model.request.admin.CreateThumbnailUploadRequest;
import com.gii.api.model.response.admin.ThumbnailUploadResponse;
import com.gii.api.service.storage.R2PresignedUrlService;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdminThumbnailUploadService {

  private static final Pattern FILE_NAME =
      Pattern.compile("(?i)[a-z0-9][a-z0-9._-]*\\.(avif|jpe?g|png|webp)");
  private static final Map<String, String> CONTENT_TYPES =
      Map.of(
          "avif", "image/avif",
          "jpg", "image/jpeg",
          "jpeg", "image/jpeg",
          "png", "image/png",
          "webp", "image/webp");

  private final R2PresignedUrlService r2PresignedUrlService;

  public ThumbnailUploadResponse execute(CreateThumbnailUploadRequest request) {
    String filename = request.filename().trim();
    if (!FILE_NAME.matcher(filename).matches()) {
      throw invalidUpload();
    }
    String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    String expectedContentType = CONTENT_TYPES.get(extension);
    if (!expectedContentType.equalsIgnoreCase(request.contentType().trim())) {
      throw invalidUpload();
    }

    int extensionStart = filename.lastIndexOf('.');
    String baseName = filename.substring(0, extensionStart);
    String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
    String storedFilename =
        baseName + "-" + uniqueSuffix + filename.substring(extensionStart).toLowerCase(Locale.ROOT);
    String objectKey = "thumbnails/" + request.ownerType().path() + "/" + storedFilename;
    R2PresignedUrlService.PresignedUpload upload =
        r2PresignedUrlService.generateUploadUrl(
            objectKey, expectedContentType, request.sizeBytes());
    return ThumbnailUploadResponse.builder()
        .objectKey(objectKey)
        .uploadUrl(upload.uploadUrl())
        .method("PUT")
        .contentType(expectedContentType)
        .sizeBytes(request.sizeBytes())
        .expiresAt(upload.expiresAt())
        .build();
  }

  private ResponseStatusException invalidUpload() {
    return new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Invalid thumbnail filename or content type");
  }
}

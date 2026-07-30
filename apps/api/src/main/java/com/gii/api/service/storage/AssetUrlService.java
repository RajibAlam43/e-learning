package com.gii.api.service.storage;

import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AssetUrlService {

  private static final Pattern THUMBNAIL_FILE_NAME =
      Pattern.compile("(?i)[a-z0-9][a-z0-9._-]*\\.(avif|jpe?g|png|webp)");

  private final String assetsBaseUrl;

  public AssetUrlService(@Value("${assets.base-url}") String assetsBaseUrl) {
    this.assetsBaseUrl = stripTrailingSlashes(assetsBaseUrl);
  }

  public String publicUrl(String objectKey) {
    if (objectKey == null || objectKey.isBlank() || assetsBaseUrl.isBlank()) {
      return null;
    }
    return assetsBaseUrl + "/" + objectKey.trim();
  }

  public String normalizeThumbnailKey(String objectKey, String expectedOwnerPath) {
    if (objectKey == null) {
      return null;
    }
    String normalized = objectKey.trim();
    if (normalized.isEmpty()) {
      return null;
    }
    if (normalized.startsWith("/")
        || normalized.contains("..")
        || normalized.contains("\\")
        || normalized.contains("://")) {
      throw invalidThumbnailKey();
    }
    String[] segments = normalized.split("/", -1);
    if (segments.length != 3
        || !"thumbnails".equals(segments[0])
        || !expectedOwnerPath.equals(segments[1])
        || !THUMBNAIL_FILE_NAME.matcher(segments[2]).matches()
        || !hasUniqueSuffix(segments[2])) {
      throw invalidThumbnailKey();
    }
    return normalized;
  }

  private ResponseStatusException invalidThumbnailKey() {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid thumbnail object key");
  }

  private static String stripTrailingSlashes(String value) {
    return value == null ? "" : value.trim().replaceAll("/+$", "");
  }

  private static boolean hasUniqueSuffix(String filename) {
    int extensionStart = filename.lastIndexOf('.');
    return extensionStart > 9
        && filename.substring(extensionStart - 9, extensionStart).matches("-[0-9a-fA-F]{8}");
  }
}

package com.gii.api.service.storage;

import java.util.UUID;
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

  public String normalizeThumbnailKey(String objectKey, String ownerType, UUID ownerId) {
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
    String requiredPrefix = ownerType + "/" + ownerId + "/thumbnails/";
    String fileName =
        normalized.startsWith(requiredPrefix)
            ? normalized.substring(requiredPrefix.length())
            : "";
    if (!THUMBNAIL_FILE_NAME.matcher(fileName).matches()) {
      throw invalidThumbnailKey();
    }
    return normalized;
  }

  public String normalizeCreateThumbnailKey(String objectKey, String ownerType) {
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
        || normalized.contains("://")
        || !Pattern.matches(
            Pattern.quote(ownerType)
                + "/[0-9a-fA-F-]{36}/thumbnails/"
                + THUMBNAIL_FILE_NAME.pattern(),
            normalized)) {
      throw invalidThumbnailKey();
    }
    return normalized;
  }

  private ResponseStatusException invalidThumbnailKey() {
    return new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Invalid thumbnail object key");
  }

  private static String stripTrailingSlashes(String value) {
    return value == null ? "" : value.trim().replaceAll("/+$", "");
  }
}

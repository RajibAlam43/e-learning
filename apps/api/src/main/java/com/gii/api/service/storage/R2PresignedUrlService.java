package com.gii.api.service.storage;

import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
public class R2PresignedUrlService {

  private final String accountId;
  private final String accessKeyId;
  private final String secretAccessKey;
  private final String bucket;
  private final long downloadTtlSeconds;
  private final String region;

  public R2PresignedUrlService(
      @Value("${storage.r2.account-id:}") String accountId,
      @Value("${storage.r2.access-key-id:}") String accessKeyId,
      @Value("${storage.r2.secret-access-key:}") String secretAccessKey,
      @Value("${storage.r2.bucket:}") String bucket,
      @Value("${storage.r2.download-url-ttl-seconds:600}") long downloadTtlSeconds,
      @Value("${storage.r2.region:auto}") String region) {
    this.accountId = accountId;
    this.accessKeyId = accessKeyId;
    this.secretAccessKey = secretAccessKey;
    this.bucket = bucket;
    this.downloadTtlSeconds = downloadTtlSeconds;
    this.region = region;
  }

  /** Builds a time-limited signed GET URL for a lesson resource stored in Cloudflare R2. */
  public PresignedDownload generateDownloadUrl(
      String fileUrlOrKey, String fileName, String mimeType) {
    String objectKey = resolveObjectKey(fileUrlOrKey);
    Duration signatureDuration = Duration.ofSeconds(downloadTtlSeconds);

    GetObjectRequest.Builder getRequest =
        GetObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .responseContentDisposition("attachment; filename=\"" + safeFileName(fileName) + "\"");

    if (mimeType != null && !mimeType.isBlank()) {
      getRequest.responseContentType(mimeType);
    }

    GetObjectPresignRequest presignRequest =
        GetObjectPresignRequest.builder()
            .signatureDuration(signatureDuration)
            .getObjectRequest(getRequest.build())
            .build();

    PresignedGetObjectRequest signed = buildPresigner().presignGetObject(presignRequest);

    return new PresignedDownload(
        signed.url().toString(), java.time.Instant.now().plus(signatureDuration));
  }

  private S3Presigner buildPresigner() {
    validateConfig();
    return S3Presigner.builder()
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
        .region(Region.of(region))
        .endpointOverride(URI.create("https://" + accountId + ".r2.cloudflarestorage.com"))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .build();
  }

  private String resolveObjectKey(String fileUrlOrKey) {
    if (fileUrlOrKey == null || fileUrlOrKey.isBlank()) {
      throw new IllegalArgumentException("Resource file URL/key is missing");
    }

    String trimmed = fileUrlOrKey.trim();
    if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
      return trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
    }

    URI uri = URI.create(trimmed);
    String path = uri.getPath() == null ? "" : uri.getPath().trim();
    if (path.isEmpty() || "/".equals(path)) {
      throw new IllegalArgumentException("Resource file URL does not contain an object path");
    }

    String normalized = path.startsWith("/") ? path.substring(1) : path;
    String bucketPrefix = bucket + "/";
    if (normalized.startsWith(bucketPrefix)) {
      return normalized.substring(bucketPrefix.length());
    }
    return normalized;
  }

  private void validateConfig() {
    if (accountId.isBlank()
        || accessKeyId.isBlank()
        || secretAccessKey.isBlank()
        || bucket.isBlank()) {
      throw new IllegalStateException("R2 storage configuration is incomplete");
    }
  }

  private String safeFileName(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      return "resource";
    }
    return fileName.replace("\"", "");
  }

  public record PresignedDownload(String downloadUrl, java.time.Instant expiresAt) {}
}

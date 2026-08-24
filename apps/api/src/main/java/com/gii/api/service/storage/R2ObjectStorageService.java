package com.gii.api.service.storage;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class R2ObjectStorageService {

  private final String accountId;
  private final String accessKeyId;
  private final String secretAccessKey;
  private final String bucket;
  private final String region;

  public R2ObjectStorageService(
      @Value("${storage.r2.account-id}") String accountId,
      @Value("${storage.r2.access-key-id}") String accessKeyId,
      @Value("${storage.r2.secret-access-key}") String secretAccessKey,
      @Value("${storage.r2.bucket}") String bucket,
      @Value("${storage.r2.region}") String region) {
    this.accountId = accountId;
    this.accessKeyId = accessKeyId;
    this.secretAccessKey = secretAccessKey;
    this.bucket = bucket;
    this.region = region;
  }

  public void put(String objectKey, byte[] content, String contentType) {
    validateConfig();
    PutObjectRequest request =
        PutObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .contentType(contentType)
            .contentLength((long) content.length)
            .build();
    try (S3Client client = buildClient()) {
      client.putObject(request, RequestBody.fromBytes(content));
    } catch (SdkException exception) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Certificate storage is temporarily unavailable", exception);
    }
  }

  public String storageLocation(String objectKey) {
    return "r2://" + bucket + "/" + objectKey;
  }

  public String bucket() {
    return bucket;
  }

  private S3Client buildClient() {
    return S3Client.builder()
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
        .region(Region.of(region))
        .endpointOverride(URI.create("https://" + accountId + ".r2.cloudflarestorage.com"))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .build();
  }

  private void validateConfig() {
    if (accountId.isBlank()
        || accessKeyId.isBlank()
        || secretAccessKey.isBlank()
        || bucket.isBlank()) {
      throw new IllegalStateException("R2 storage configuration is incomplete");
    }
  }
}

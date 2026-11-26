package com.gii.api.service.media;

import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.NoSuchUploadException;
import software.amazon.awssdk.services.s3.model.Part;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

@Service
public class RecordingStorageService {
  public static final long MAX_BYTES = 8L * 1024 * 1024 * 1024;
  public static final long PART_BYTES = 8L * 1024 * 1024;

  @Value("${recordings.s3.bucket:}")
  private String bucket;

  @Value("${recordings.s3.region:ap-southeast-1}")
  private String region;

  @Value("${recordings.s3.access-key-id:}")
  private String accessKey;

  @Value("${recordings.s3.secret-access-key:}")
  private String secretKey;

  public S3Client client() {
    return S3Client.builder()
        .region(Region.of(region))
        .credentialsProvider(
            accessKey.isBlank()
                ? DefaultCredentialsProvider.builder().build()
                : StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)))
        .build();
  }

  public String begin(String key) {
    try (S3Client s3 = client()) {
      return s3.createMultipartUpload(
              CreateMultipartUploadRequest.builder()
                  .bucket(bucket)
                  .key(key)
                  .contentType("video/mp4")
                  .build())
          .uploadId();
    }
  }

  public String partUrl(String key, String uploadId, int number, long bytes) {
    if (number < 1 || number > 1024 || bytes < 1 || bytes > PART_BYTES) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid recording part number");
    }
    try (S3Presigner signer =
        S3Presigner.builder()
            .region(Region.of(region))
            .credentialsProvider(
                accessKey.isBlank()
                    ? DefaultCredentialsProvider.builder().build()
                    : StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
            .build()) {
      return signer
          .presignUploadPart(
              UploadPartPresignRequest.builder()
                  .signatureDuration(Duration.ofMinutes(10))
                  .uploadPartRequest(
                      UploadPartRequest.builder()
                          .bucket(bucket)
                          .key(key)
                          .uploadId(uploadId)
                          .partNumber(number)
                          .contentLength(bytes)
                          .build())
                  .build())
          .url()
          .toString();
    }
  }

  public void complete(String key, String uploadId, int count, long bytes) {
    if (count < 1 || count > 1024 || bytes < 1 || bytes > MAX_BYTES) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid recording size");
    }
    try (S3Client s3 = client()) {
      try {
        List<Part> parts =
            s3
                .listPartsPaginator(
                    ListPartsRequest.builder().bucket(bucket).key(key).uploadId(uploadId).build())
                .parts()
                .stream()
                .toList();
        long actualBytes = 0;
        if (parts.size() != count) {
          throw incomplete();
        }
        for (int i = 0; i < parts.size(); i++) {
          Part part = parts.get(i);
          if (part.partNumber() != i + 1
              || part.size() > PART_BYTES
              || (i < parts.size() - 1 && part.size() != PART_BYTES)) {
            throw incomplete();
          }
          actualBytes += part.size();
        }
        if (actualBytes != bytes) {
          throw incomplete();
        }
        s3.completeMultipartUpload(
            CompleteMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .multipartUpload(
                    CompletedMultipartUpload.builder()
                        .parts(
                            parts.stream()
                                .map(p ->
                                    CompletedPart.builder()
                                        .partNumber(p.partNumber())
                                        .eTag(p.eTag())
                                        .build())
                                .toList())
                        .build())
                .build());
      } catch (NoSuchUploadException alreadyCompleted) {
        // Completion can succeed before the HTTP response or DB commit is lost.
      }
      if (s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build()).contentLength()
          != bytes) {
        throw incomplete();
      }
    }
  }

  public void abort(String key, String uploadId) {
    try (S3Client s3 = client()) {
      s3.abortMultipartUpload(
          AbortMultipartUploadRequest.builder().bucket(bucket).key(key).uploadId(uploadId).build());
    } catch (NoSuchUploadException ignored) {
      // An already-aborted upload needs no further action.
    }
  }

  private ResponseStatusException incomplete() {
    return new ResponseStatusException(
        HttpStatus.CONFLICT, "Recording upload is incomplete. Retry uploading before finishing.");
  }
}

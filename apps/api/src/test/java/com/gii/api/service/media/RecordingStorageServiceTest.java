package com.gii.api.service.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.ListPartsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchUploadException;
import software.amazon.awssdk.services.s3.model.Part;
import software.amazon.awssdk.services.s3.paginators.ListPartsIterable;

class RecordingStorageServiceTest {
  private final RecordingStorageService service = spy(new RecordingStorageService());
  private final S3Client s3 = mock(S3Client.class);

  @BeforeEach
  void setup() {
    ReflectionTestUtils.setField(service, "bucket", "private-recordings");
    ReflectionTestUtils.setField(service, "region", "ap-southeast-1");
    ReflectionTestUtils.setField(service, "accessKey", "test-access-key");
    ReflectionTestUtils.setField(service, "secretKey", "test-secret-key");
    doReturn(s3).when(service).client();
    when(s3.listPartsPaginator(any(ListPartsRequest.class)))
        .thenAnswer(call -> new ListPartsIterable(s3, call.getArgument(0)));
  }

  @Test
  void presignsOnlyTheSpecifiedPartWithItsSizeBoundToTheSignature() {
    String url = service.partUrl("recordings/class/id/source.mp4", "upload-id", 1, 4096);
    assertThat(url)
        .contains(
            "private-recordings.s3.ap-southeast-1.amazonaws.com",
            "partNumber=1",
            "uploadId=upload-id",
            "content-length",
            "X-Amz-Signature=");
    assertThatThrownBy(
            () -> service.partUrl("key", "id", 1, RecordingStorageService.PART_BYTES + 1))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void validatesActualS3PartsAndHeadBeforeCompleting() {
    long size = RecordingStorageService.PART_BYTES + 100;
    parts(List.of(part(1, RecordingStorageService.PART_BYTES), part(2, 100)));
    when(s3.headObject(any(HeadObjectRequest.class)))
        .thenReturn(HeadObjectResponse.builder().contentLength(size).build());
    service.complete("key", "upload-id", 2, size);
    verify(s3).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
  }

  @Test
  void rejectsMissingPartsAndUndersizedNonFinalParts() {
    parts(List.of(part(1, 100), part(3, 100)));
    assertThatThrownBy(() -> service.complete("key", "id", 3, 300))
        .isInstanceOf(ResponseStatusException.class);
    parts(List.of(part(1, 100), part(2, 100)));
    assertThatThrownBy(() -> service.complete("key", "id", 2, 200))
        .isInstanceOf(ResponseStatusException.class);
    verify(s3, never()).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
  }

  @Test
  void recoversCompletedUploadAfterLostResponse() {
    when(s3.listParts(any(ListPartsRequest.class)))
        .thenThrow(NoSuchUploadException.builder().message("completed").build());
    when(s3.headObject(any(HeadObjectRequest.class)))
        .thenReturn(HeadObjectResponse.builder().contentLength(100L).build());
    service.complete("key", "id", 1, 100);
    verify(s3, never()).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
  }

  private void parts(List<Part> parts) {
    when(s3.listParts(any(ListPartsRequest.class)))
        .thenReturn(ListPartsResponse.builder().parts(parts).isTruncated(false).build());
  }

  private Part part(int number, long bytes) {
    return Part.builder().partNumber(number).size(bytes).eTag("etag-" + number).build();
  }
}

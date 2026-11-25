package com.gii.worker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

class RecordingPackagingServiceTest {
  private final ObjectMapper mapper = new ObjectMapper();

  @TempDir Path temporary;

  @Test
  void uploadsPlaybackInBoundedPartsAndCompletesWithReturnedTags() throws Exception {
    S3Client s3 = multipartClient();
    Path file = temporary.resolve("playback.mp4");
    byte[] bytes = new byte[8 * 1024 * 1024 + 7];
    bytes[bytes.length - 1] = 42;
    Files.write(file, bytes);
    List<byte[]> received = new ArrayList<>();
    when(s3.uploadPart(any(UploadPartRequest.class), any(RequestBody.class)))
        .thenAnswer(invocation -> {
          RequestBody body = invocation.getArgument(1);
          try (var input = body.contentStreamProvider().newStream()) {
            received.add(input.readAllBytes());
          }
          return UploadPartResponse.builder().eTag("tag-" + received.size()).build();
        });
    packaging().uploadMultipart(s3, "recordings/test/playback.mp4", file, "video/mp4");
    assertThat(received).hasSize(2);
    assertThat(received.get(0)).hasSize(8 * 1024 * 1024);
    assertThat(received.get(1)).hasSize(7);
    assertThat(received.get(1)[6]).isEqualTo((byte) 42);
    var complete = ArgumentCaptor.forClass(CompleteMultipartUploadRequest.class);
    verify(s3).completeMultipartUpload(complete.capture());
    assertThat(complete.getValue().uploadId()).isEqualTo("upload");
    assertThat(complete.getValue().multipartUpload().parts())
        .extracting(p -> p.eTag())
        .containsExactly("tag-1", "tag-2");
    verify(s3, never()).abortMultipartUpload(any(AbortMultipartUploadRequest.class));
  }

  @Test
  void abortsPlaybackMultipartUploadWhenTransferFails() throws Exception {
    S3Client s3 = multipartClient();
    Path file = temporary.resolve("failed.mp4");
    Files.write(file, new byte[7]);
    when(s3.uploadPart(any(UploadPartRequest.class), any(RequestBody.class)))
        .thenThrow(new IllegalStateException("transfer failed"));
    assertThatThrownBy(() -> packaging().uploadMultipart(s3, "recordings/test", file, "video/mp4"))
        .isInstanceOf(IllegalStateException.class);
    verify(s3).abortMultipartUpload(any(AbortMultipartUploadRequest.class));
    verify(s3, never()).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
  }

  private S3Client multipartClient() {
    S3Client s3 = mock(S3Client.class);
    when(s3.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
        .thenReturn(CreateMultipartUploadResponse.builder().uploadId("upload").build());
    return s3;
  }

  private RecordingPackagingService packaging() {
    RecordingPackagingService service = new RecordingPackagingService(null, null);
    ReflectionTestUtils.setField(service, "bucket", "test-bucket");
    return service;
  }

  @Test
  void acceptsH264AndAacForStreamCopy() throws Exception {
    assertThat(
            RecordingPackagingService.validateProbe(mapper.readTree(probe("h264", "aac", "3600"))))
        .isEqualTo(3600);
  }

  @Test
  void rejectsFormatsThatWouldRequireTranscoding() {
    assertThatThrownBy(() ->
            RecordingPackagingService.validateProbe(mapper.readTree(probe("vp9", "opus", "60"))))
        .isInstanceOf(IOException.class);
    assertThatThrownBy(() ->
            RecordingPackagingService.validateProbe(mapper.readTree(probe("h264", "opus", "60"))))
        .isInstanceOf(IOException.class);
  }

  @Test
  void rejectsMissingAudioInvalidDurationAndOversizedDimensions() {
    for (String json :
        new String[] {
          "{}",
          probe("h264", "aac", "0"),
          probe("h264", "aac", "30000"),
          probe("h264", "aac", "NaN"),
          probe("h264", "aac", "60").replace("1280", "3840"),
          "{\"streams\":[{\"codec_type\":\"video\",\"codec_name\":\"h264\","
              + "\"width\":1280,\"height\":720}],\"format\":{\"duration\":60}}"
        }) {
      assertThatThrownBy(() -> RecordingPackagingService.validateProbe(mapper.readTree(json)))
          .isInstanceOf(IOException.class);
    }
  }

  private String probe(String video, String audio, String duration) {
    return ("{\"streams\":[{\"codec_type\":\"video\",\"codec_name\":\"%s\","
            + "\"width\":1280,\"height\":720},{\"codec_type\":\"audio\",\"codec_name\":\"%s\"}],"
            + "\"format\":{\"duration\":\"%s\"}}")
        .formatted(video, audio, duration);
  }
}

package com.gii.worker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gii.common.entity.live.LiveClassRecording;
import com.gii.common.enums.RecordingStatus;
import com.gii.common.repository.live.LiveClassRecordingRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "recordings.enabled", havingValue = "true")
public class RecordingPackagingService {
  private static final long MAX_BYTES = 8L * 1024 * 1024 * 1024;
  private final LiveClassRecordingRepository recordings;
  private final TransactionTemplate transactions;

  @Value("${recordings.s3.bucket:}")
  private String bucket;

  @Value("${recordings.s3.region:ap-southeast-1}")
  private String region;

  @Value("${recordings.s3.access-key-id:}")
  private String accessKey;

  @Value("${recordings.s3.secret-access-key:}")
  private String secretKey;

  @Value("${recordings.ffmpeg:ffmpeg}")
  private String ffmpeg;

  @Value("${recordings.ffprobe:ffprobe}")
  private String ffprobe;

  @Configuration
  @EnableScheduling
  @ConditionalOnProperty(name = "recordings.enabled", havingValue = "true")
  static class RecordingScheduling {}

  @Scheduled(fixedDelayString = "${recordings.poll-delay-ms:15000}")
  public void processNext() {
    LiveClassRecording job =
        transactions.execute(status ->
            recordings
                .claimNext()
                .map(recording -> {
                  recording.setAttempts(recording.getAttempts() + 1);
                  recording.setStatus(RecordingStatus.PROCESSING);
                  recording.setProcessingClaim(UUID.randomUUID());
                  recording.setProcessingUntil(Instant.now().plusSeconds(7200));
                  return recordings.saveAndFlush(recording);
                })
                .orElse(null));
    if (job == null) {
      return;
    }
    Path directory = null;
    try {
      if (job.getAttempts() > 3) {
        throw new IOException("Recording processing retry limit reached");
      }
      directory = Files.createTempDirectory("class-recording-");
      Files.createDirectory(directory.resolve("hls"));
      Path input = directory.resolve("source.mp4");
      try (S3Client s3 = s3()) {
        String key = job.getObjectPrefix() + "source.mp4";
        long bytes =
            s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build())
                .contentLength();
        if (bytes < 1 || bytes > MAX_BYTES || !Long.valueOf(bytes).equals(job.getSizeBytes())) {
          throw new IOException("Recording size does not match its completed upload");
        }
        if (directory.toFile().getUsableSpace() < bytes * 4 + 1024L * 1024 * 1024) {
          throw new IOException("Insufficient temporary disk space for recording packaging");
        }
        s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build(), input);
        Path probeFile = directory.resolve("probe.json");
        run(
            List.of(
                ffprobe,
                "-v",
                "error",
                "-protocol_whitelist",
                "file,pipe",
                "-f",
                "mov",
                "-show_streams",
                "-show_format",
                "-of",
                "json",
                input.toString()),
            probeFile,
            120);
        final double duration = validateProbe(new ObjectMapper().readTree(probeFile.toFile()));
        run(
            List.of(
                ffmpeg,
                "-nostdin",
                "-v",
                "error",
                "-y",
                "-protocol_whitelist",
                "file,pipe",
                "-f",
                "mov",
                "-i",
                input.toString(),
                "-map",
                "0:v:0",
                "-map",
                "0:a:0",
                "-c",
                "copy",
                "-f",
                "hls",
                "-hls_time",
                "6",
                "-hls_playlist_type",
                "vod",
                "-hls_segment_type",
                "fmp4",
                "-hls_fmp4_init_filename",
                "init.mp4",
                "-hls_segment_filename",
                directory.resolve("hls/segment-%05d.m4s").toString(),
                directory.resolve("hls/index.m3u8").toString(),
                "-map",
                "0:v:0",
                "-map",
                "0:a:0",
                "-c",
                "copy",
                "-movflags",
                "+faststart",
                directory.resolve("playback.mp4").toString()),
            directory.resolve("package.log"),
            1200);
        Path hls = directory.resolve("hls");
        String playlist = Files.readString(hls.resolve("index.m3u8"));
        if (!playlist.contains("#EXT-X-ENDLIST") || !playlist.contains("#EXTINF:")) {
          throw new IOException("Recording did not produce a complete HLS playlist");
        }
        try (var files = Files.list(hls)) {
          for (Path file :
              files
                  .sorted(
                      Comparator.comparing(p -> p.getFileName().toString().equals("index.m3u8")))
                  .toList()) {
            String name = file.getFileName().toString();
            put(
                s3,
                job.getObjectPrefix() + "hls/" + name,
                file,
                name.endsWith(".m3u8") ? "application/vnd.apple.mpegurl" : "video/mp4");
          }
        }
        put(
            s3,
            job.getObjectPrefix() + "playback.mp4",
            directory.resolve("playback.mp4"),
            "video/mp4");
        transactions.executeWithoutResult(status ->
            recordings
                .findLocked(job.getId())
                .ifPresent(recording -> {
                  if (!job.getProcessingClaim().equals(recording.getProcessingClaim())) {
                    return;
                  }
                  recording.setDurationSeconds(duration);
                  recording.setStatus(RecordingStatus.READY);
                  recording.setPublishedAt(Instant.now());
                  recording.setProcessingUntil(null);
                  recording.setFailureMessage(null);
                }));
      }
    } catch (Exception error) {
      if (error instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      log.warn("Recording {} packaging failed ({})", job.getId(), error.getClass().getSimpleName());
      transactions.executeWithoutResult(status ->
          recordings
              .findLocked(job.getId())
              .ifPresent(recording -> {
                if (!job.getProcessingClaim().equals(recording.getProcessingClaim())) {
                  return;
                }
                recording.setStatus(
                    recording.getAttempts() >= 3 ? RecordingStatus.FAILED : RecordingStatus.QUEUED);
                recording.setProcessingUntil(null);
                recording.setFailureMessage(
                    "Recording validation or packaging failed. Contact support with its ID.");
              }));
    } finally {
      if (directory != null) {
        try (var files = Files.walk(directory)) {
          for (Path file : files.sorted(Comparator.reverseOrder()).toList()) {
            Files.deleteIfExists(file);
          }
        } catch (IOException cleanupError) {
          log.warn("Could not remove recording temporary files");
        }
      }
    }
  }

  static double validateProbe(JsonNode probe) throws IOException {
    boolean video = false;
    boolean audio = false;
    for (JsonNode stream : probe.path("streams")) {
      if ("video".equals(stream.path("codec_type").asText())) {
        if (!"h264".equals(stream.path("codec_name").asText())
            || stream.path("width").asInt() < 1
            || stream.path("height").asInt() < 1
            || stream.path("width").asInt() > 1920
            || stream.path("height").asInt() > 1080) {
          throw new IOException("Unsupported recording video format");
        }
        video = true;
      }
      if ("audio".equals(stream.path("codec_type").asText())) {
        if (!"aac".equals(stream.path("codec_name").asText())) {
          throw new IOException("Unsupported recording audio format");
        }
        audio = true;
      }
    }
    double duration = probe.path("format").path("duration").asDouble();
    if (!video || !audio || !Double.isFinite(duration) || duration < 1 || duration > 28800) {
      throw new IOException("Recording must contain H.264 video and AAC audio, up to eight hours");
    }
    return duration;
  }

  private void run(List<String> command, Path output, int seconds)
      throws IOException, InterruptedException {
    Process process =
        new ProcessBuilder(command)
            .redirectErrorStream(true)
            .redirectOutput(output.toFile())
            .start();
    try {
      if (!process.waitFor(seconds, TimeUnit.SECONDS)) {
        throw new IOException("Recording tool timed out");
      }
      if (process.exitValue() != 0) {
        throw new IOException("Recording tool rejected the media");
      }
    } finally {
      if (process.isAlive()) {
        process.destroyForcibly();
      }
    }
  }

  private void put(S3Client s3, String key, Path path, String type) throws IOException {
    if (Files.size(path) > 128L * 1024 * 1024) {
      uploadMultipart(s3, key, path, type);
      return;
    }
    s3.putObject(
        PutObjectRequest.builder().bucket(bucket).key(key).contentType(type).build(),
        RequestBody.fromFile(path));
  }

  void uploadMultipart(S3Client s3, String key, Path path, String type) throws IOException {
    String uploadId =
        s3.createMultipartUpload(
                CreateMultipartUploadRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(type)
                    .build())
            .uploadId();
    try {
      List<CompletedPart> parts = new ArrayList<>();
      try (var input = Files.newInputStream(path)) {
        byte[] bytes;
        while ((bytes = input.readNBytes(8 * 1024 * 1024)).length > 0) {
          int number = parts.size() + 1;
          String entityTag =
              s3.uploadPart(
                      UploadPartRequest.builder()
                          .bucket(bucket)
                          .key(key)
                          .uploadId(uploadId)
                          .partNumber(number)
                          .build(),
                      RequestBody.fromBytes(bytes))
                  .eTag();
          parts.add(CompletedPart.builder().partNumber(number).eTag(entityTag).build());
        }
      }
      s3.completeMultipartUpload(
          CompleteMultipartUploadRequest.builder()
              .bucket(bucket)
              .key(key)
              .uploadId(uploadId)
              .multipartUpload(CompletedMultipartUpload.builder().parts(parts).build())
              .build());
    } catch (IOException | RuntimeException error) {
      try {
        s3.abortMultipartUpload(
            AbortMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .build());
      } catch (RuntimeException cleanup) {
        error.addSuppressed(cleanup);
      }
      throw error;
    }
  }

  private S3Client s3() {
    return S3Client.builder()
        .region(Region.of(region))
        .credentialsProvider(
            accessKey.isBlank()
                ? DefaultCredentialsProvider.builder().build()
                : StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)))
        .overrideConfiguration(c -> c.apiCallTimeout(java.time.Duration.ofMinutes(15)))
        .build();
  }
}

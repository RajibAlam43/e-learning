package com.gii.api.service.admin;

import com.gii.api.model.request.admin.CreateLessonVideoUploadRequest;
import com.gii.api.model.response.admin.LessonVideoUploadResponse;
import com.gii.api.service.media.MuxDirectUploadClient;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.course.MuxVideoUpload;
import com.gii.common.enums.LessonType;
import com.gii.common.enums.MuxUploadStatus;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.course.MuxVideoUploadRepository;
import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminLessonVideoUploadService {

  private static final int MIN_UPLOAD_TIMEOUT_SECONDS = 60;
  private static final int MAX_UPLOAD_TIMEOUT_SECONDS = 604800;

  private static final Pattern FILE_NAME =
      Pattern.compile("(?i)[a-z0-9][a-z0-9._-]*\\.(mp4|webm|mov)");
  private static final Map<String, String> CONTENT_TYPES =
      Map.of("mp4", "video/mp4", "webm", "video/webm", "mov", "video/quicktime");

  private final LessonRepository lessonRepository;
  private final MuxDirectUploadClient muxDirectUploadClient;
  private final MuxVideoUploadRepository muxVideoUploadRepository;

  public LessonVideoUploadResponse execute(UUID lessonId, CreateLessonVideoUploadRequest request) {
    Lesson lesson =
        lessonRepository
            .findById(lessonId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
    if (lesson.getLessonType() != LessonType.VIDEO) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Videos can only be uploaded to VIDEO lessons");
    }

    String filename = request.filename().trim();
    if (!FILE_NAME.matcher(filename).matches()) {
      throw invalidUpload();
    }
    int extensionStart = filename.lastIndexOf('.');
    String extension = filename.substring(extensionStart + 1).toLowerCase(Locale.ROOT);
    String contentType = CONTENT_TYPES.get(extension);
    if (contentType == null || !contentType.equalsIgnoreCase(request.contentType().trim())) {
      throw invalidUpload();
    }

    MuxDirectUploadClient.DirectUpload upload =
        muxDirectUploadClient.create(lessonId.toString(), lesson.getTitle());
    if (!isValidUpload(upload)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Mux returned an invalid direct upload URL");
    }
    int timeoutSeconds = upload.timeout() != null ? upload.timeout() : 3600;
    Instant expiresAt = Instant.now().plusSeconds(timeoutSeconds);
    muxVideoUploadRepository.saveAndFlush(
        MuxVideoUpload.builder()
            .lesson(lesson)
            .uploadId(upload.id())
            .status(MuxUploadStatus.WAITING)
            .filename(filename)
            .contentType(contentType)
            .sizeBytes(request.sizeBytes())
            .expiresAt(expiresAt)
            .build());
    return LessonVideoUploadResponse.builder()
        .uploadId(upload.id())
        .uploadUrl(upload.url())
        .method("PUT")
        .contentType(contentType)
        .sizeBytes(request.sizeBytes())
        .expiresAt(expiresAt)
        .build();
  }

  private boolean isValidUpload(MuxDirectUploadClient.DirectUpload upload) {
    if (upload == null
        || upload.id() == null
        || upload.id().isBlank()
        || upload.url() == null
        || upload.url().isBlank()
        || (upload.timeout() != null
            && (upload.timeout() < MIN_UPLOAD_TIMEOUT_SECONDS
                || upload.timeout() > MAX_UPLOAD_TIMEOUT_SECONDS))) {
      return false;
    }
    try {
      URI uri = URI.create(upload.url());
      return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null;
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }

  private ResponseStatusException invalidUpload() {
    return new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Video filename and content type must describe MP4, WebM, or MOV");
  }
}

package com.gii.api.service.media;

import com.gii.api.model.request.instructor.StartRecordingRequest;
import com.gii.api.model.response.RecordingResponse;
import com.gii.api.model.response.RecordingResponse.ClassRecordings;
import com.gii.api.model.response.RecordingResponse.PartUrl;
import com.gii.api.model.response.RecordingResponse.Playback;
import com.gii.api.model.response.RecordingResponse.UploadSession;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.entity.live.LiveClass;
import com.gii.common.entity.live.LiveClassRecording;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.LiveClassProvider;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.RecordingStatus;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.live.LiveClassRecordingRepository;
import com.gii.common.repository.live.LiveClassRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class LiveClassRecordingService {
  private final CurrentUserService currentUserService;
  private final LiveClassRepository classes;
  private final LiveClassRecordingRepository recordings;
  private final EnrollmentRepository enrollments;
  private final RecordingStorageService storage;
  private final RecordingPlaybackService playback;

  @Value("${recordings.enabled:false}")
  private boolean enabled;

  @Value("${recordings.s3.bucket:}")
  private String bucket;

  public ClassRecordings list(UUID classId, Authentication auth) {
    LiveClass liveClass = requireTeacher(classId, auth);
    return new ClassRecordings(
        enabled && !bucket.isBlank(),
        liveClass.getTitle(),
        liveClass.effectiveHostStartUrl(),
        recordings.findByLiveClassIdOrderByCreatedAtDesc(classId).stream()
            .map(this::response)
            .toList());
  }

  @Transactional(readOnly = true)
  public java.util.List<RecordingResponse> published(UUID classId, Authentication auth) {
    requireEnabled();
    LiveClass liveClass = classes.findById(classId).orElseThrow(this::missing);
    requireViewer(liveClass, auth);
    return recordings.findByLiveClassIdOrderByCreatedAtDesc(classId).stream()
        .filter(r -> r.getStatus() == RecordingStatus.READY)
        .map(this::response)
        .toList();
  }

  public UploadSession start(UUID classId, StartRecordingRequest request, Authentication auth) {
    requireEnabled();
    LiveClass liveClass = requireTeacher(classId, auth);
    if (classes
        .findByIdAssignedToInstructor(classId, currentUserService.getCurrentUserId(auth))
        .isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Recording requires an assigned teacher");
    }
    if (liveClass.getProvider() != LiveClassProvider.GOOGLE_MEET
        || liveClass.getStatus() == LiveClassStatus.CANCELLED) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Choose a Google Meet class that is not cancelled");
    }
    LiveClassRecording recording =
        LiveClassRecording.builder()
            .liveClass(liveClass)
            .ownerId(currentUserService.getCurrentUserId(auth))
            .method(request.method())
            .mimeType(request.mimeType())
            .status(RecordingStatus.UPLOADING)
            .objectPrefix("recordings/" + classId + "/" + UUID.randomUUID() + "/")
            .build();
    recordings.saveAndFlush(recording);
    String uploadId = storage.begin(source(recording));
    try {
      recording.setUploadId(uploadId);
      UploadSession session = rotateToken(recording);
      recordings.saveAndFlush(recording);
      return session;
    } catch (RuntimeException error) {
      try {
        storage.abort(source(recording), uploadId);
      } catch (RuntimeException cleanup) {
        error.addSuppressed(cleanup);
      }
      throw error;
    }
  }

  public UploadSession resume(UUID id, Authentication auth) {
    requireEnabled();
    LiveClassRecording recording = recordings.findLocked(id).orElseThrow(this::missing);
    requireTeacher(recording.getLiveClass().getId(), auth);
    if (!recording.getOwnerId().equals(currentUserService.getCurrentUserId(auth))) {
      throw missing();
    }
    requireUploading(recording);
    return rotateToken(recording);
  }

  public PartUrl part(UUID id, int number, long bytes, String token) {
    LiveClassRecording recording = authorizeUpload(id, token);
    requireUploading(recording);
    return new PartUrl(storage.partUrl(source(recording), recording.getUploadId(), number, bytes));
  }

  public RecordingResponse complete(UUID id, int parts, long bytes, String token) {
    LiveClassRecording recording = authorizeUpload(id, token);
    if (recording.getStatus() != RecordingStatus.UPLOADING) {
      if (recording.getSizeBytes() == null || recording.getSizeBytes() != bytes) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "Recording was already finalized differently");
      }
      return response(recording);
    }
    storage.complete(source(recording), recording.getUploadId(), parts, bytes);
    recording.setSizeBytes(bytes);
    recording.setStatus(RecordingStatus.QUEUED);
    return response(recording);
  }

  public RecordingResponse abort(UUID id, String token) {
    LiveClassRecording recording = authorizeUpload(id, token);
    requireUploading(recording);
    storage.abort(source(recording), recording.getUploadId());
    recording.setStatus(RecordingStatus.FAILED);
    recording.setFailureMessage("Recording cancelled by the teacher");
    return response(recording);
  }

  @Transactional(readOnly = true)
  public Playback playback(
      UUID id, Authentication auth, HttpServletRequest request, HttpServletResponse httpResponse) {
    requireEnabled();
    LiveClassRecording recording = recordings.findById(id).orElseThrow(this::missing);
    if (recording.getStatus() != RecordingStatus.READY) {
      throw missing();
    }
    requireViewer(recording.getLiveClass(), auth);
    return playback.issue(recording, request, httpResponse);
  }

  private void requireViewer(LiveClass liveClass, Authentication auth) {
    UUID userId = currentUserService.getCurrentUserId(auth);
    UUID classId = liveClass.getId();
    if (!isAdmin(auth) && classes.findByIdAssignedToInstructor(classId, userId).isEmpty()) {
      var enrollment =
          enrollments
              .findByUserIdAndCourseIdAndStatus(
                  userId, liveClass.getCourse().getId(), EnrollmentStatus.ACTIVE)
              .orElseThrow(this::missing);
      if ((enrollment.getExpiresAt() != null && !enrollment.getExpiresAt().isAfter(Instant.now()))
          || liveClass.getCourse().getStatus() != PublishStatus.PUBLISHED
          || liveClass.getSection().getStatus() != PublishStatus.PUBLISHED
          || liveClass.getStatus() == LiveClassStatus.CANCELLED) {
        throw missing();
      }
    }
  }

  private LiveClassRecording authorizeUpload(UUID id, String token) {
    requireEnabled();
    LiveClassRecording recording = recordings.findLocked(id).orElseThrow(this::missing);
    if (token == null
        || token.length() > 100
        || recording.getUploadTokenHash() == null
        || recording.getUploadExpiresAt() == null
        || !recording.getUploadExpiresAt().isAfter(Instant.now())
        || !MessageDigest.isEqual(
            hash(token).getBytes(StandardCharsets.UTF_8),
            recording.getUploadTokenHash().getBytes(StandardCharsets.UTF_8))) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Recording upload authorization expired");
    }
    if (classes
        .findByIdAssignedToInstructor(recording.getLiveClass().getId(), recording.getOwnerId())
        .isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Teacher is no longer assigned to this class");
    }
    return recording;
  }

  private UploadSession rotateToken(LiveClassRecording recording) {
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    Instant expiry = Instant.now().plusSeconds(86400);
    recording.setUploadTokenHash(hash(token));
    recording.setUploadExpiresAt(expiry);
    return new UploadSession(
        recording.getId(),
        token,
        expiry,
        recording.getMimeType(),
        RecordingStorageService.PART_BYTES,
        RecordingStorageService.MAX_BYTES);
  }

  private String hash(String token) {
    try {
      return Base64.getEncoder()
          .encodeToString(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException error) {
      throw new IllegalStateException(error);
    }
  }

  private LiveClass requireTeacher(UUID id, Authentication auth) {
    UUID userId = currentUserService.getCurrentUserId(auth);
    return (isAdmin(auth) ? classes.findById(id) : classes.findByIdAssignedToInstructor(id, userId))
        .orElseThrow(this::missing);
  }

  private boolean isAdmin(Authentication auth) {
    return auth.getAuthorities().stream()
        .anyMatch(a ->
            a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
  }

  private void requireEnabled() {
    if (!enabled || bucket.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Recording pilot is not configured yet");
    }
  }

  private void requireUploading(LiveClassRecording recording) {
    if (recording.getStatus() != RecordingStatus.UPLOADING) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Recording upload is already closed");
    }
  }

  private String source(LiveClassRecording recording) {
    return recording.getObjectPrefix() + "source.mp4";
  }

  private ResponseStatusException missing() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Recording or class not found");
  }

  private RecordingResponse response(LiveClassRecording recording) {
    return new RecordingResponse(
        recording.getId(),
        recording.getMethod(),
        recording.getStatus(),
        recording.getCreatedAt(),
        recording.getDurationSeconds(),
        recording.getFailureMessage());
  }
}

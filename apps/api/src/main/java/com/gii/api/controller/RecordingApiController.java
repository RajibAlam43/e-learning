package com.gii.api.controller;

import com.gii.api.model.request.instructor.StartRecordingRequest;
import com.gii.api.model.response.RecordingResponse;
import com.gii.api.model.response.RecordingResponse.ClassRecordings;
import com.gii.api.model.response.RecordingResponse.PartUrl;
import com.gii.api.model.response.RecordingResponse.Playback;
import com.gii.api.model.response.RecordingResponse.UploadSession;
import com.gii.api.service.media.LiveClassRecordingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RecordingApiController implements RecordingApi {
  private final LiveClassRecordingService service;

  @Override
  public ResponseEntity<java.util.List<RecordingResponse>> published(
      UUID classId, Authentication auth) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(service.published(classId, auth));
  }

  @Override
  public ResponseEntity<ClassRecordings> list(UUID classId, Authentication auth) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(service.list(classId, auth));
  }

  @Override
  public ResponseEntity<UploadSession> start(
      UUID classId, StartRecordingRequest request, Authentication auth) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(service.start(classId, request, auth));
  }

  @Override
  public ResponseEntity<UploadSession> resume(UUID id, Authentication auth) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.resume(id, auth));
  }

  @Override
  public ResponseEntity<PartUrl> part(UUID id, int number, long bytes, String token) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(service.part(id, number, bytes, token));
  }

  @Override
  public ResponseEntity<RecordingResponse> complete(UUID id, int parts, long bytes, String token) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(service.complete(id, parts, bytes, token));
  }

  @Override
  public ResponseEntity<RecordingResponse> abort(UUID id, String token) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.abort(id, token));
  }

  @Override
  public ResponseEntity<Playback> playback(
      UUID id, Authentication auth, HttpServletRequest request, HttpServletResponse response) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(service.playback(id, auth, request, response));
  }
}

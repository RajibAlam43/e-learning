package com.gii.api.controller;

import com.gii.api.model.request.instructor.StartRecordingRequest;
import com.gii.api.model.response.RecordingResponse;
import com.gii.api.model.response.RecordingResponse.ClassRecordings;
import com.gii.api.model.response.RecordingResponse.PartUrl;
import com.gii.api.model.response.RecordingResponse.Playback;
import com.gii.api.model.response.RecordingResponse.UploadSession;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Class recordings", description = "Browser and extension recording pilot")
public interface RecordingApi {
  @GetMapping("/live-classes/{classId}/recordings")
  @Operation(summary = "List automatically published recordings after class access checks")
  ResponseEntity<java.util.List<RecordingResponse>> published(
      @PathVariable UUID classId, Authentication auth);

  @GetMapping("/instructor/live-classes/{classId}/recordings")
  @PreAuthorize("hasAnyRole('INSTRUCTOR', 'TEACHER', 'ADMIN', 'SUPER_ADMIN')")
  @Operation(summary = "List recordings for an assigned class")
  ResponseEntity<ClassRecordings> list(@PathVariable UUID classId, Authentication auth);

  @PostMapping("/instructor/live-classes/{classId}/recordings")
  @PreAuthorize("hasAnyRole('INSTRUCTOR', 'TEACHER', 'ADMIN', 'SUPER_ADMIN')")
  @Operation(summary = "Start a private multipart recording upload")
  ResponseEntity<UploadSession> start(
      @PathVariable UUID classId,
      @RequestBody @Valid StartRecordingRequest request,
      Authentication auth);

  @PostMapping("/instructor/recordings/{id}/resume")
  @PreAuthorize("hasAnyRole('INSTRUCTOR', 'TEACHER', 'ADMIN', 'SUPER_ADMIN')")
  @Operation(summary = "Renew the owner's recording upload authorization")
  ResponseEntity<UploadSession> resume(@PathVariable UUID id, Authentication auth);

  @PostMapping("/recording-uploads/{id}/parts/{number}")
  @Operation(summary = "Authorize one recording part using a scoped upload token")
  ResponseEntity<PartUrl> part(
      @PathVariable UUID id,
      @PathVariable @Min(1) @Max(1024) int number,
      @RequestParam @Min(1) @Max(8388608) long bytes,
      @RequestHeader("X-Recording-Token") String token);

  @PostMapping("/recording-uploads/{id}/complete")
  @Operation(summary = "Verify uploaded parts and queue recording validation")
  ResponseEntity<RecordingResponse> complete(
      @PathVariable UUID id,
      @RequestParam @Min(1) @Max(1024) int parts,
      @RequestParam @Min(1) @Max(8589934592L) long bytes,
      @RequestHeader("X-Recording-Token") String token);

  @PostMapping("/recording-uploads/{id}/abort")
  @Operation(summary = "Cancel an unfinished recording upload")
  ResponseEntity<RecordingResponse> abort(
      @PathVariable UUID id, @RequestHeader("X-Recording-Token") String token);

  @GetMapping("/recordings/{id}/playback")
  @Operation(summary = "Authorize private HLS playback after enrollment or teacher checks")
  ResponseEntity<Playback> playback(
      @PathVariable UUID id,
      Authentication auth,
      HttpServletRequest request,
      HttpServletResponse response);
}

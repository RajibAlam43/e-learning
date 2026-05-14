package com.gii.api.controller;

import com.gii.api.model.request.admin.CreateLiveClassRequest;
import com.gii.api.model.request.admin.UpdateLiveClassRequest;
import com.gii.api.model.response.live.LiveClassStartResponse;
import com.gii.api.model.response.live.LiveClassUpsertResponse;
import com.gii.api.model.response.admin.AdminLiveClassDetailResponse;
import com.gii.api.model.response.admin.AdminLiveClassStartResponse;
import com.gii.api.model.response.instructor.InstructorLiveClassResponse;
import com.gii.api.model.response.instructor.InstructorLiveClassStartResponse;
import com.gii.api.model.response.student.StudentLiveClassJoinResponse;
import com.gii.api.service.admin.AdminLiveClassManagementService;
import com.gii.api.service.instructor.InstructorLiveClassService;
import com.gii.api.service.student.StudentJoinLiveClassesService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/live-classes")
public class LiveClassApiController {

  private final InstructorLiveClassService instructorLiveClassService;
  private final AdminLiveClassManagementService adminLiveClassManagementService;
  private final StudentJoinLiveClassesService studentJoinLiveClassesService;

  @PostMapping("/courses/{courseId}")
  public ResponseEntity<LiveClassUpsertResponse> create(
      @PathVariable UUID courseId,
      @RequestBody @Valid com.gii.api.model.request.instructor.CreateLiveClassRequest request,
      Authentication authentication) {
    if (hasRole(authentication, "ROLE_ADMIN")) {
      CreateLiveClassRequest adminRequest =
          CreateLiveClassRequest.builder()
              .sectionId(request.sectionId())
              .title(request.title())
              .description(request.description())
              .startsAt(request.startsAt())
              .endsAt(request.endsAt())
              .provider(request.provider())
              .maxCapacity(request.maxCapacity())
              .build();
      return ResponseEntity.ok(toUpsertResponse(adminLiveClassManagementService.create(courseId, adminRequest)));
    }
    if (hasRole(authentication, "ROLE_INSTRUCTOR")) {
      return ResponseEntity.ok(
          toUpsertResponse(instructorLiveClassService.create(courseId, request, authentication)));
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin/instructor can create");
  }

  @PatchMapping("/{liveClassId}")
  public ResponseEntity<LiveClassUpsertResponse> update(
      @PathVariable UUID liveClassId,
      @RequestBody @Valid com.gii.api.model.request.instructor.UpdateLiveClassRequest request,
      Authentication authentication) {
    if (hasRole(authentication, "ROLE_ADMIN")) {
      UpdateLiveClassRequest adminRequest =
          UpdateLiveClassRequest.builder()
              .title(request.title())
              .description(request.description())
              .startsAt(request.startsAt())
              .endsAt(request.endsAt())
              .status(request.status() != null ? request.status().name() : null)
              .build();
      return ResponseEntity.ok(
          toUpsertResponse(adminLiveClassManagementService.update(liveClassId, adminRequest)));
    }
    if (hasRole(authentication, "ROLE_INSTRUCTOR")) {
      return ResponseEntity.ok(
          toUpsertResponse(instructorLiveClassService.update(liveClassId, request, authentication)));
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin/instructor can update");
  }

  @PostMapping("/{liveClassId}/start")
  public ResponseEntity<LiveClassStartResponse> start(
      @PathVariable UUID liveClassId, Authentication authentication) {
    if (hasRole(authentication, "ROLE_ADMIN")) {
      return ResponseEntity.ok(toStartResponse(adminLiveClassManagementService.start(liveClassId)));
    }
    if (hasRole(authentication, "ROLE_INSTRUCTOR")) {
      return ResponseEntity.ok(
          toStartResponse(instructorLiveClassService.start(liveClassId, authentication)));
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin/instructor can start");
  }

  @DeleteMapping("/{liveClassId}")
  public ResponseEntity<LiveClassUpsertResponse> cancel(
      @PathVariable UUID liveClassId, Authentication authentication) {
    if (hasRole(authentication, "ROLE_ADMIN")) {
      return ResponseEntity.ok(toUpsertResponse(adminLiveClassManagementService.cancel(liveClassId)));
    }
    if (hasRole(authentication, "ROLE_INSTRUCTOR")) {
      return ResponseEntity.ok(
          toUpsertResponse(instructorLiveClassService.cancel(liveClassId, authentication)));
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin/instructor can cancel");
  }

  @PostMapping("/{liveClassId}/join")
  public ResponseEntity<StudentLiveClassJoinResponse> join(
      @PathVariable UUID liveClassId, Authentication authentication) {
    if (!hasRole(authentication, "ROLE_STUDENT") && !hasRole(authentication, "ROLE_ADMIN")) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only student/admin can join");
    }
    return ResponseEntity.ok(studentJoinLiveClassesService.execute(liveClassId, authentication));
  }

  private boolean hasRole(Authentication authentication, String role) {
    return authentication.getAuthorities().stream()
        .anyMatch(a -> role.equals(a.getAuthority()));
  }

  private LiveClassUpsertResponse toUpsertResponse(AdminLiveClassDetailResponse response) {
    return LiveClassUpsertResponse.builder()
        .liveClassId(response.liveClassId())
        .title(response.title())
        .description(response.description())
        .courseId(response.courseId())
        .courseName(response.courseName())
        .sectionId(response.sectionId())
        .sectionTitle(response.sectionTitle())
        .startsAt(response.startsAt())
        .endsAt(response.endsAt())
        .provider(response.provider())
        .status(Enum.valueOf(com.gii.common.enums.LiveClassStatus.class, response.status()))
        .meetingId(response.meetingId())
        .hostStartUrl(response.hostStartUrl())
        .joinUrl(response.joinUrl())
        .createdAt(response.createdAt())
        .updatedAt(response.updatedAt())
        .build();
  }

  private LiveClassUpsertResponse toUpsertResponse(InstructorLiveClassResponse response) {
    return LiveClassUpsertResponse.builder()
        .liveClassId(response.liveClassId())
        .title(response.title())
        .description(response.description())
        .courseId(response.courseId())
        .courseName(response.courseName())
        .sectionId(response.sectionId())
        .sectionTitle(response.sectionTitle())
        .startsAt(response.startsAt())
        .endsAt(response.endsAt())
        .provider(response.provider())
        .status(response.status())
        .meetingId(response.meetingId())
        .hostStartUrl(response.hostStartUrl())
        .joinUrl(response.joinUrl())
        .createdAt(response.createdAt())
        .updatedAt(response.updatedAt())
        .build();
  }

  private LiveClassStartResponse toStartResponse(AdminLiveClassStartResponse response) {
    return LiveClassStartResponse.builder()
        .liveClassId(response.liveClassId())
        .title(response.title())
        .provider(response.provider())
        .hostStartUrl(response.hostStartUrl())
        .meetingId(response.meetingId())
        .startsAt(response.startsAt())
        .endsAt(response.endsAt())
        .status(Enum.valueOf(com.gii.common.enums.LiveClassStatus.class, response.status()))
        .registeredStudents(response.registeredStudents())
        .recordingEnabled(response.recordingEnabled())
        .build();
  }

  private LiveClassStartResponse toStartResponse(InstructorLiveClassStartResponse response) {
    return LiveClassStartResponse.builder()
        .liveClassId(response.liveClassId())
        .title(response.title())
        .provider(response.provider())
        .hostStartUrl(response.hostStartUrl())
        .meetingId(response.meetingId())
        .startsAt(response.startsAt())
        .endsAt(response.endsAt())
        .status(response.status())
        .registeredStudents(response.registeredStudents())
        .recordingEnabled(response.recordingEnabled())
        .build();
  }
}

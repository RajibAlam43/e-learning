package com.gii.api.controller;

import com.gii.api.model.request.admin.CreateLiveClassRequest;
import com.gii.api.model.request.admin.UpdateLiveClassRequest;
import com.gii.api.model.response.admin.AdminLiveClassDetailResponse;
import com.gii.api.model.response.admin.AdminLiveClassStartResponse;
import com.gii.api.model.response.instructor.InstructorLiveClassResponse;
import com.gii.api.model.response.instructor.InstructorLiveClassStartResponse;
import com.gii.api.model.response.live.LiveClassStartResponse;
import com.gii.api.model.response.live.LiveClassUpsertResponse;
import com.gii.api.model.response.student.StudentLiveClassJoinResponse;
import com.gii.api.service.admin.AdminLiveClassManagementService;
import com.gii.api.service.instructor.InstructorLiveClassService;
import com.gii.api.service.student.StudentJoinLiveClassesService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LiveClassApiController implements LiveClassApi {

  private final InstructorLiveClassService instructorLiveClassService;
  private final AdminLiveClassManagementService adminLiveClassManagementService;
  private final StudentJoinLiveClassesService studentJoinLiveClassesService;

  @Override
  public ResponseEntity<LiveClassUpsertResponse> create(
      UUID courseId,
      com.gii.api.model.request.instructor.CreateLiveClassRequest request,
      Authentication authentication) {
    if (hasRole(authentication, "ROLE_ADMIN")) {
      CreateLiveClassRequest adminRequest =
          CreateLiveClassRequest.builder()
              .sectionId(request.sectionId())
              .title(request.title())
              .titleEn(request.titleEn())
              .description(request.description())
              .descriptionEn(request.descriptionEn())
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
    throw new IllegalStateException("Authenticated role not eligible for create");
  }

  @Override
  public ResponseEntity<LiveClassUpsertResponse> update(
      UUID liveClassId,
      com.gii.api.model.request.instructor.UpdateLiveClassRequest request,
      Authentication authentication) {
    if (hasRole(authentication, "ROLE_ADMIN")) {
      UpdateLiveClassRequest adminRequest =
          UpdateLiveClassRequest.builder()
              .title(request.title())
              .titleEn(request.titleEn())
              .description(request.description())
              .descriptionEn(request.descriptionEn())
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
    throw new IllegalStateException("Authenticated role not eligible for update");
  }

  @Override
  public ResponseEntity<LiveClassStartResponse> start(
      UUID liveClassId, Authentication authentication) {
    if (hasRole(authentication, "ROLE_ADMIN")) {
      return ResponseEntity.ok(toStartResponse(adminLiveClassManagementService.start(liveClassId)));
    }
    if (hasRole(authentication, "ROLE_INSTRUCTOR")) {
      return ResponseEntity.ok(
          toStartResponse(instructorLiveClassService.start(liveClassId, authentication)));
    }
    throw new IllegalStateException("Authenticated role not eligible for start");
  }

  @Override
  public ResponseEntity<LiveClassUpsertResponse> cancel(
      UUID liveClassId, Authentication authentication) {
    if (hasRole(authentication, "ROLE_ADMIN")) {
      return ResponseEntity.ok(toUpsertResponse(adminLiveClassManagementService.cancel(liveClassId)));
    }
    if (hasRole(authentication, "ROLE_INSTRUCTOR")) {
      return ResponseEntity.ok(
          toUpsertResponse(instructorLiveClassService.cancel(liveClassId, authentication)));
    }
    throw new IllegalStateException("Authenticated role not eligible for cancel");
  }

  @Override
  public ResponseEntity<StudentLiveClassJoinResponse> join(
      UUID liveClassId, Authentication authentication) {
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

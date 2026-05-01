package com.gii.api.controller;

import com.gii.api.model.request.instructor.CreateLiveClassRequest;
import com.gii.api.model.request.instructor.UpdateLiveClassRequest;
import com.gii.api.model.response.instructor.InstructorDashboardResponse;
import com.gii.api.model.response.instructor.InstructorLiveClassResponse;
import com.gii.api.model.response.instructor.InstructorLiveClassStartResponse;
import com.gii.api.service.instructor.InstructorDashboardService;
import com.gii.api.service.instructor.InstructorLiveClassService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InstructorApiController implements InstructorApi {

  private final InstructorDashboardService instructorDashboardService;
  private final InstructorLiveClassService instructorLiveClassService;

  @Override
  public ResponseEntity<InstructorDashboardResponse> getDashboard(Authentication authentication) {
    return ResponseEntity.ok(instructorDashboardService.execute(authentication));
  }

  @Override
  public ResponseEntity<InstructorLiveClassResponse> createLiveClass(
      UUID courseId, CreateLiveClassRequest request, Authentication authentication) {
    return ResponseEntity.ok(instructorLiveClassService.create(courseId, request, authentication));
  }

  @Override
  public ResponseEntity<InstructorLiveClassStartResponse> startLiveClass(
      UUID liveClassId, Authentication authentication) {
    return ResponseEntity.ok(instructorLiveClassService.start(liveClassId, authentication));
  }

  @Override
  public ResponseEntity<InstructorLiveClassResponse> updateLiveClass(
      UUID liveClassId, UpdateLiveClassRequest request, Authentication authentication) {
    return ResponseEntity.ok(
        instructorLiveClassService.update(liveClassId, request, authentication));
  }

  @Override
  public ResponseEntity<Void> deleteLiveClass(UUID liveClassId, Authentication authentication) {
    instructorLiveClassService.delete(liveClassId, authentication);
    return ResponseEntity.ok().build();
  }
}

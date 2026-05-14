package com.gii.api.controller;

import com.gii.api.model.response.instructor.InstructorDashboardResponse;
import com.gii.api.service.instructor.InstructorDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InstructorApiController implements InstructorApi {

  private final InstructorDashboardService instructorDashboardService;

  @Override
  public ResponseEntity<InstructorDashboardResponse> getDashboard(Authentication authentication) {
    return ResponseEntity.ok(instructorDashboardService.execute(authentication));
  }
}

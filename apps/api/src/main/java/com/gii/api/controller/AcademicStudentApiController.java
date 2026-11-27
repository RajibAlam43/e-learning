package com.gii.api.controller;

import com.gii.api.model.response.academic.AcademicRecordResponse;
import com.gii.api.service.academic.AcademicAchievementService;
import com.gii.api.service.enrollment.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AcademicStudentApiController implements AcademicStudentApi {
  private final AcademicAchievementService achievements;
  private final CurrentUserService users;

  @Override
  public ResponseEntity<AcademicRecordResponse> academicRecord(Authentication authentication) {
    return ResponseEntity.ok(achievements.get(users.getCurrentUserId(authentication)));
  }
}

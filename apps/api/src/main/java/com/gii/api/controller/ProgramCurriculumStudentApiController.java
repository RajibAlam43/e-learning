package com.gii.api.controller;

import com.gii.api.model.response.academic.StudentProgramCurriculumResponse;
import com.gii.api.service.academic.StudentProgramCurriculumService;
import com.gii.api.service.enrollment.CurrentUserService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProgramCurriculumStudentApiController implements ProgramCurriculumStudentApi {
  private final StudentProgramCurriculumService programs;
  private final CurrentUserService users;
  private final com.gii.api.service.academic.ProgramCourseRegistrationService registrations;

  @Override
  public ResponseEntity<com.gii.api.model.response.academic.ProgramRegistrationResponse> register(
      UUID id,
      com.gii.api.model.request.admin.RegisterProgramCourseRequest request,
      Authentication authentication) {
    return ResponseEntity.ok(
        registrations.register(users.getCurrentUserId(authentication), id, request.courseId()));
  }

  @Override
  public ResponseEntity<StudentProgramCurriculumResponse> curriculum(
      UUID id, Authentication authentication) {
    return ResponseEntity.ok(programs.get(users.getCurrentUserId(authentication), id));
  }
}

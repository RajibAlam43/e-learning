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

  @Override
  public ResponseEntity<StudentProgramCurriculumResponse> curriculum(
      UUID id, Authentication authentication) {
    return ResponseEntity.ok(programs.get(users.getCurrentUserId(authentication), id));
  }
}

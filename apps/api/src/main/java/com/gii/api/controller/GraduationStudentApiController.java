package com.gii.api.controller;

import com.gii.api.model.response.academic.AcademicProgressResponse;
import com.gii.api.model.response.academic.ProgramGraduationResponse;
import com.gii.api.service.academic.AcademicProgressService;
import com.gii.api.service.enrollment.CurrentUserService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GraduationStudentApiController implements GraduationStudentApi {
  private final AcademicProgressService progress;
  private final CurrentUserService users;

  @Override
  public ResponseEntity<AcademicProgressResponse> progress(Authentication auth) {
    return ResponseEntity.ok(progress.get(users.getCurrentUserId(auth)));
  }

  @Override
  public ResponseEntity<ProgramGraduationResponse> program(UUID programId, Authentication auth) {
    return ResponseEntity.ok(progress.getProgram(users.getCurrentUserId(auth), programId));
  }

  @Override
  public ResponseEntity<ProgramGraduationResponse> check(UUID programId, Authentication auth) {
    return ResponseEntity.ok(progress.check(users.getCurrentUserId(auth), programId));
  }
}

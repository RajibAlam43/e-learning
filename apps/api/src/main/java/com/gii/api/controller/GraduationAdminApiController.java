package com.gii.api.controller;

import com.gii.api.model.response.academic.AcademicProgressResponse;
import com.gii.api.model.response.academic.ProgramGraduationResponse;
import com.gii.api.service.academic.AcademicProgressService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GraduationAdminApiController implements GraduationAdminApi {
  private final AcademicProgressService progress;

  @Override
  public ResponseEntity<AcademicProgressResponse> progress(UUID userId) {
    return ResponseEntity.ok(progress.get(userId));
  }

  @Override
  public ResponseEntity<ProgramGraduationResponse> check(UUID userId, UUID programId) {
    return ResponseEntity.ok(progress.check(userId, programId));
  }
}

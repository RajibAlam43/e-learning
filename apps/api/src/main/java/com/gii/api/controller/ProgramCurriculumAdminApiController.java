package com.gii.api.controller;

import com.gii.api.model.request.admin.SaveProgramCurriculumRequest;
import com.gii.api.model.response.admin.AdminCollectionDetailResponse;
import com.gii.api.service.academic.ProgramCurriculumService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProgramCurriculumAdminApiController implements ProgramCurriculumAdminApi {
  private final ProgramCurriculumService curriculum;

  @Override
  public ResponseEntity<AdminCollectionDetailResponse> saveCurriculum(
      UUID id, SaveProgramCurriculumRequest request) {
    return ResponseEntity.ok(curriculum.save(id, request));
  }
}

package com.gii.api.controller;

import com.gii.api.model.request.admin.SaveProgramCurriculumRequest;
import com.gii.api.model.response.admin.AdminCollectionDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Program curriculum")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/admin/collections")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public interface ProgramCurriculumAdminApi {
  @PutMapping("/{id}/curriculum")
  @Operation(summary = "Save program graduation requirements and mandatory/elective courses")
  ResponseEntity<AdminCollectionDetailResponse> saveCurriculum(
      @PathVariable UUID id, @Valid @RequestBody SaveProgramCurriculumRequest request);
}

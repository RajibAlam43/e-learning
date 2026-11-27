package com.gii.api.controller;

import com.gii.api.model.request.admin.ReviewWaiverRequest;
import com.gii.api.model.request.admin.SavePrerequisitesRequest;
import com.gii.api.model.response.academic.PrerequisiteResponse;
import com.gii.api.model.response.academic.WaiverResponse;
import com.gii.api.service.academic.CoursePrerequisiteManagementService;
import com.gii.api.service.academic.CourseWaiverService;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.api.service.storage.R2PresignedUrlService;
import com.gii.common.enums.WaiverStatus;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PrerequisiteAdminApiController implements PrerequisiteAdminApi {
  private final CoursePrerequisiteManagementService rules;
  private final CourseWaiverService waivers;
  private final CurrentUserService users;

  @Override
  public ResponseEntity<PrerequisiteResponse> prerequisites(UUID courseId) {
    return ResponseEntity.ok(rules.get(courseId));
  }

  @Override
  public ResponseEntity<PrerequisiteResponse> savePrerequisites(
      UUID courseId, SavePrerequisitesRequest request) {
    return ResponseEntity.ok(rules.save(courseId, request.courseTemplateIds()));
  }

  @Override
  public ResponseEntity<List<WaiverResponse>> waivers(WaiverStatus status, int page) {
    return ResponseEntity.ok(waivers.list(status, page));
  }

  @Override
  public ResponseEntity<WaiverResponse> review(
      UUID id, ReviewWaiverRequest request, Authentication authentication) {
    return ResponseEntity.ok(waivers.review(id, users.getCurrentUserId(authentication), request));
  }

  @Override
  public ResponseEntity<R2PresignedUrlService.PresignedDownload> evidence(
      UUID id, Authentication authentication) {
    return ResponseEntity.ok(waivers.download(id, users.getCurrentUserId(authentication), true));
  }
}

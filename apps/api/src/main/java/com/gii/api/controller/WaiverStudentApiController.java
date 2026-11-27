package com.gii.api.controller;

import com.gii.api.model.request.admin.CreateWaiverRequest;
import com.gii.api.model.request.admin.WaiverEvidenceUploadRequest;
import com.gii.api.model.response.academic.PrerequisiteResponse.CourseOption;
import com.gii.api.model.response.academic.WaiverResponse;
import com.gii.api.service.academic.CoursePrerequisiteManagementService;
import com.gii.api.service.academic.CourseWaiverService;
import com.gii.api.service.academic.WaiverEvidenceService;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.api.service.storage.R2PresignedUrlService;
import com.gii.common.service.payment.CoursePrerequisiteEligibilityService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WaiverStudentApiController implements WaiverStudentApi {
  private final CoursePrerequisiteManagementService rules;
  private final CourseWaiverService waivers;
  private final CurrentUserService users;
  private final WaiverEvidenceService evidence;
  private final CoursePrerequisiteEligibilityService eligibility;

  @Override
  public ResponseEntity<List<CourseOption>> courses() {
    return ResponseEntity.ok(rules.waiverCourses());
  }

  @Override
  public ResponseEntity<List<WaiverResponse>> waivers(Authentication authentication, int page) {
    return ResponseEntity.ok(waivers.mine(users.getCurrentUserId(authentication), page));
  }

  @Override
  public ResponseEntity<WaiverResponse> request(
      Authentication authentication, CreateWaiverRequest request) {
    return ResponseEntity.ok(waivers.create(users.getCurrentUserId(authentication), request));
  }

  @Override
  public ResponseEntity<WaiverEvidenceService.Upload> upload(
      Authentication authentication, WaiverEvidenceUploadRequest request) {
    return ResponseEntity.ok(evidence.upload(users.getCurrentUserId(authentication), request));
  }

  @Override
  public ResponseEntity<R2PresignedUrlService.PresignedDownload> evidence(
      UUID id, Authentication authentication) {
    return ResponseEntity.ok(waivers.download(id, users.getCurrentUserId(authentication), false));
  }

  @Override
  public ResponseEntity<List<CoursePrerequisiteEligibilityService.Requirement>> eligibility(
      UUID courseId, Authentication authentication) {
    return ResponseEntity.ok(
        eligibility.missing(
            users.getCurrentUserId(authentication), rules.get(courseId).courseTemplateId()));
  }
}

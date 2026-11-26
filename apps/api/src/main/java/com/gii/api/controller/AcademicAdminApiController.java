package com.gii.api.controller;

import com.gii.api.model.request.admin.RepeatCourseRequest;
import com.gii.api.model.request.admin.SaveSemesterRequest;
import com.gii.api.model.response.academic.AcademicRecordResponse;
import com.gii.api.model.response.academic.SemesterResponse;
import com.gii.api.model.response.admin.AdminCourseDetailResponse;
import com.gii.api.service.academic.AcademicAchievementService;
import com.gii.api.service.academic.SemesterService;
import com.gii.api.service.admin.AdminCourseManagementService;
import com.gii.common.repository.user.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class AcademicAdminApiController implements AcademicAdminApi {
  private final SemesterService semesters;
  private final AcademicAchievementService achievements;
  private final AdminCourseManagementService courses;
  private final UserRepository users;

  @Override
  public ResponseEntity<List<SemesterResponse>> listSemesters() {
    return ResponseEntity.ok(semesters.list());
  }

  @Override
  public ResponseEntity<SemesterResponse> createSemester(SaveSemesterRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(semesters.save(null, request));
  }

  @Override
  public ResponseEntity<SemesterResponse> updateSemester(
      UUID semesterId, SaveSemesterRequest request) {
    return ResponseEntity.ok(semesters.save(semesterId, request));
  }

  @Override
  public ResponseEntity<AcademicRecordResponse> academicRecord(UUID userId) {
    if (!users.existsById(userId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");
    }
    return ResponseEntity.ok(achievements.get(userId));
  }

  @Override
  public ResponseEntity<List<AdminCourseDetailResponse>> listOfferings(UUID courseId) {
    return ResponseEntity.ok(courses.listOfferings(courseId));
  }

  @Override
  public ResponseEntity<AdminCourseDetailResponse> createOffering(
      UUID courseId, RepeatCourseRequest request, Authentication authentication) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(courses.createOffering(courseId, request, authentication));
  }
}

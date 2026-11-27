package com.gii.api.controller;

import com.gii.api.model.request.admin.SaveFacultyGroupRequest;
import com.gii.api.model.request.admin.SaveFacultyMemberRequest;
import com.gii.api.model.response.faculty.FacultyGroupResponse;
import com.gii.api.model.response.faculty.FacultyMemberResponse;
import com.gii.api.service.faculty.FacultyDirectoryService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminFacultyApiController implements AdminFacultyApi {
  private final FacultyDirectoryService service;

  @Override
  public ResponseEntity<List<FacultyGroupResponse>> listFaculty() {
    return ResponseEntity.ok(service.list(true));
  }

  @Override
  public ResponseEntity<FacultyGroupResponse> createFacultyGroup(SaveFacultyGroupRequest request) {
    return ResponseEntity.ok(service.saveGroup(null, request));
  }

  @Override
  public ResponseEntity<FacultyGroupResponse> updateFacultyGroup(
      UUID id, SaveFacultyGroupRequest request) {
    return ResponseEntity.ok(service.saveGroup(id, request));
  }

  @Override
  public ResponseEntity<Void> deleteFacultyGroup(UUID id) {
    service.deleteGroup(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<FacultyMemberResponse> createFacultyMember(
      SaveFacultyMemberRequest request) {
    return ResponseEntity.ok(service.saveMember(null, request));
  }

  @Override
  public ResponseEntity<FacultyMemberResponse> updateFacultyMember(
      UUID id, SaveFacultyMemberRequest request) {
    return ResponseEntity.ok(service.saveMember(id, request));
  }

  @Override
  public ResponseEntity<Void> deleteFacultyMember(UUID id) {
    service.deleteMember(id);
    return ResponseEntity.noContent().build();
  }
}

package com.gii.api.controller;

import com.gii.api.model.response.faculty.FacultyGroupResponse;
import com.gii.api.model.response.faculty.FacultyMemberResponse;
import com.gii.api.service.faculty.FacultyDirectoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FacultyPublicApiController implements FacultyPublicApi {
  private final FacultyDirectoryService service;

  @Override
  public ResponseEntity<List<FacultyGroupResponse>> publicFaculty() {
    return ResponseEntity.ok(service.list(false));
  }

  @Override
  public ResponseEntity<List<FacultyMemberResponse>> featuredFaculty() {
    return ResponseEntity.ok(service.featured());
  }
}

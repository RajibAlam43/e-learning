package com.gii.api.controller;

import com.gii.api.model.response.faculty.FacultyGroupResponse;
import com.gii.api.model.response.faculty.FacultyMemberResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@Tag(name = "Public Faculty")
public interface FacultyPublicApi {
  @GetMapping("/public/faculty")
  @Operation(summary = "List visible faculty grouped in display order")
  ResponseEntity<List<FacultyGroupResponse>> publicFaculty();

  @GetMapping("/public/faculty/featured")
  @Operation(summary = "List homepage faculty independent of directory visibility")
  ResponseEntity<List<FacultyMemberResponse>> featuredFaculty();
}

package com.gii.api.controller;

import com.gii.api.model.request.admin.SaveFacultyGroupRequest;
import com.gii.api.model.request.admin.SaveFacultyMemberRequest;
import com.gii.api.model.response.faculty.FacultyGroupResponse;
import com.gii.api.model.response.faculty.FacultyMemberResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Admin Faculty")
@RequestMapping("/admin/faculty")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public interface AdminFacultyApi {
  @GetMapping
  @Operation(summary = "List faculty groups and people, including hidden entries")
  ResponseEntity<List<FacultyGroupResponse>> listFaculty();

  @PostMapping("/groups")
  @Operation(summary = "Create a faculty group")
  ResponseEntity<FacultyGroupResponse> createFacultyGroup(
      @Valid @RequestBody SaveFacultyGroupRequest request);

  @PutMapping("/groups/{id}")
  @Operation(summary = "Edit faculty group details, visibility and order")
  ResponseEntity<FacultyGroupResponse> updateFacultyGroup(
      @PathVariable UUID id, @Valid @RequestBody SaveFacultyGroupRequest request);

  @DeleteMapping("/groups/{id}")
  @Operation(summary = "Remove an empty faculty group")
  ResponseEntity<Void> deleteFacultyGroup(@PathVariable UUID id);

  @PostMapping("/members")
  @Operation(summary = "Add a teacher or manual faculty profile")
  ResponseEntity<FacultyMemberResponse> createFacultyMember(
      @Valid @RequestBody SaveFacultyMemberRequest request);

  @PutMapping("/members/{id}")
  @Operation(summary = "Edit faculty profile, group, visibility and order")
  ResponseEntity<FacultyMemberResponse> updateFacultyMember(
      @PathVariable UUID id, @Valid @RequestBody SaveFacultyMemberRequest request);

  @DeleteMapping("/members/{id}")
  @Operation(summary = "Remove a faculty entry without deleting a teacher account")
  ResponseEntity<Void> deleteFacultyMember(@PathVariable UUID id);
}

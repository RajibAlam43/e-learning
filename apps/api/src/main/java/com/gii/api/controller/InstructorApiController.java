package com.gii.api.controller;

import com.gii.api.model.request.instructor.CreateCourseAnnouncementRequest;
import com.gii.api.model.response.CourseAnnouncementResponse;
import com.gii.api.model.response.instructor.InstructorDashboardResponse;
import com.gii.api.service.instructor.CourseAnnouncementCreationService;
import com.gii.api.service.instructor.InstructorDashboardService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InstructorApiController implements InstructorApi {

  private final InstructorDashboardService instructorDashboardService;
  private final CourseAnnouncementCreationService courseAnnouncementCreationService;

  @Override
  public ResponseEntity<InstructorDashboardResponse> getDashboard(Authentication authentication) {
    return ResponseEntity.ok(instructorDashboardService.execute(authentication));
  }

  @Override
  public ResponseEntity<CourseAnnouncementResponse> createCourseAnnouncement(
      UUID courseId, CreateCourseAnnouncementRequest request, Authentication authentication) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(courseAnnouncementCreationService.execute(courseId, request, authentication));
  }
}

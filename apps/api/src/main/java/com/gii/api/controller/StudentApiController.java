package com.gii.api.controller;

import com.gii.api.model.response.student.StudentCertificateSummaryResponse;
import com.gii.api.model.response.student.StudentCourseHomeResponse;
import com.gii.api.model.response.student.StudentCourseSummaryResponse;
import com.gii.api.model.response.student.StudentDashboardResponse;
import com.gii.api.model.response.student.StudentLiveClassJoinResponse;
import com.gii.api.model.response.student.StudentLiveClassSummaryResponse;
import com.gii.api.model.response.student.StudentOrderSummaryResponse;
import com.gii.api.service.student.CourseLiveClassesService;
import com.gii.api.service.student.EnrolledCourseDetailsService;
import com.gii.api.service.student.EnrolledCoursesService;
import com.gii.api.service.student.StudentCertificatesService;
import com.gii.api.service.student.StudentDashboardService;
import com.gii.api.service.student.StudentJoinLiveClassesService;
import com.gii.api.service.student.StudentOrdersService;
import com.gii.api.service.student.StudentUpcomingLiveClasses;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StudentApiController implements StudentApi {

  private final StudentDashboardService studentDashboardService;
  private final EnrolledCoursesService enrolledCoursesService;
  private final EnrolledCourseDetailsService enrolledCourseDetailsService;
  private final StudentOrdersService studentOrdersService;
  private final StudentCertificatesService studentCertificatesService;
  private final StudentUpcomingLiveClasses studentUpcomingLiveClasses;
  private final CourseLiveClassesService courseLiveClassesService;
  private final StudentJoinLiveClassesService studentJoinLiveClassesService;

  @Override
  public ResponseEntity<StudentDashboardResponse> getDashboard(Authentication authentication) {
    return ResponseEntity.ok(studentDashboardService.execute(authentication));
  }

  @Override
  public ResponseEntity<List<StudentCourseSummaryResponse>> getMyCourses(
      Authentication authentication) {
    return ResponseEntity.ok(enrolledCoursesService.execute(authentication));
  }

  @Override
  public ResponseEntity<StudentCourseHomeResponse> getMyCourseDetails(
      UUID courseId, Authentication authentication) {
    return ResponseEntity.ok(enrolledCourseDetailsService.execute(courseId, authentication));
  }

  @Override
  public ResponseEntity<List<StudentOrderSummaryResponse>> getMyOrders(
      Authentication authentication) {
    return ResponseEntity.ok(studentOrdersService.execute(authentication));
  }

  @Override
  public ResponseEntity<List<StudentCertificateSummaryResponse>> getMyCertificates(
      Authentication authentication) {
    return ResponseEntity.ok(studentCertificatesService.execute(authentication));
  }

  @Override
  public ResponseEntity<List<StudentLiveClassSummaryResponse>> getUpcomingLiveClasses(
      Authentication authentication) {
    return ResponseEntity.ok(studentUpcomingLiveClasses.execute(authentication));
  }

  @Override
  public ResponseEntity<List<StudentLiveClassSummaryResponse>> getCourseLiveClasses(
      UUID courseId, Authentication authentication) {
    return ResponseEntity.ok(courseLiveClassesService.execute(courseId, authentication));
  }

  @Override
  public ResponseEntity<StudentLiveClassJoinResponse> joinLiveClass(
      UUID liveClassId, Authentication authentication) {
    return ResponseEntity.ok(studentJoinLiveClassesService.execute(liveClassId, authentication));
  }
}

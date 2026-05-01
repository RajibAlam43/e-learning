package com.gii.api.controller;

import com.gii.api.model.request.CreateSupportTicketRequest;
import com.gii.api.model.response.CourseDetailsResponse;
import com.gii.api.model.response.CourseSummaryResponse;
import com.gii.api.model.response.InstructorDetailsResponse;
import com.gii.api.model.response.InstructorSummaryResponse;
import com.gii.api.model.response.PageResponse;
import com.gii.api.service.pub.AllCoursesService;
import com.gii.api.service.pub.AllInstructorsService;
import com.gii.api.service.pub.CourseDetailsService;
import com.gii.api.service.pub.InstructorDetailsService;
import com.gii.api.service.pub.SupportTicketService;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PublicApiController implements PublicApi {

  private final AllCoursesService allCoursesService;
  private final CourseDetailsService courseDetailsService;
  private final AllInstructorsService allInstructorsService;
  private final InstructorDetailsService instructorDetailsService;
  private final SupportTicketService supportTicketService;

  @Override
  public ResponseEntity<PageResponse<CourseSummaryResponse>> getAllCourses(
      UUID categoryId, CourseLevel level, CourseLanguage language, Pageable pageable) {
    return ResponseEntity.ok(allCoursesService.execute(categoryId, level, language, pageable));
  }

  @Override
  public ResponseEntity<CourseDetailsResponse> getCourseDetails(String slug) {
    return ResponseEntity.ok(courseDetailsService.execute(slug));
  }

  @Override
  public ResponseEntity<List<InstructorSummaryResponse>> getAllInstructors() {
    return ResponseEntity.ok(allInstructorsService.execute());
  }

  @Override
  public ResponseEntity<InstructorDetailsResponse> getInstructorDetails(String slug) {
    return ResponseEntity.ok(instructorDetailsService.execute(slug));
  }

  @Override
  public ResponseEntity<Void> createSupportTicket(CreateSupportTicketRequest request) {
    supportTicketService.execute(request);
    return ResponseEntity.ok().build();
  }
}

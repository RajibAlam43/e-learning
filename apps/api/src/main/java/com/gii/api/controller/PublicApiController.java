package com.gii.api.controller;

import com.gii.api.model.request.CreateSupportTicketRequest;
import com.gii.api.model.response.CategoryResponse;
import com.gii.api.model.response.CollectionDetailsResponse;
import com.gii.api.model.response.CollectionSummaryResponse;
import com.gii.api.model.response.CourseDetailsResponse;
import com.gii.api.model.response.CourseReviewResponse;
import com.gii.api.model.response.CourseSummaryResponse;
import com.gii.api.model.response.InstructorDetailsResponse;
import com.gii.api.model.response.InstructorSummaryResponse;
import com.gii.api.model.response.PageResponse;
import com.gii.api.model.response.SupportTicketCreatedResponse;
import com.gii.api.service.pub.AllCategoriesService;
import com.gii.api.service.pub.AllCollectionsService;
import com.gii.api.service.pub.AllCoursesService;
import com.gii.api.service.pub.AllInstructorsService;
import com.gii.api.service.pub.CollectionDetailsService;
import com.gii.api.service.pub.CourseDetailsService;
import com.gii.api.service.pub.CourseReviewsService;
import com.gii.api.service.pub.InstructorDetailsService;
import com.gii.api.service.pub.SupportTicketService;
import com.gii.common.enums.CollectionType;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PublicApiController implements PublicApi {

  private final AllCoursesService allCoursesService;
  private final AllCategoriesService allCategoriesService;
  private final AllCollectionsService allCollectionsService;
  private final CourseDetailsService courseDetailsService;
  private final CourseReviewsService courseReviewsService;
  private final CollectionDetailsService collectionDetailsService;
  private final AllInstructorsService allInstructorsService;
  private final InstructorDetailsService instructorDetailsService;
  private final SupportTicketService supportTicketService;

  @Override
  public ResponseEntity<List<CategoryResponse>> getAllCategories() {
    return ResponseEntity.ok(allCategoriesService.execute());
  }

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
  public ResponseEntity<List<CourseReviewResponse>> getCourseReviews(String slug) {
    return ResponseEntity.ok(courseReviewsService.execute(slug));
  }

  @Override
  public ResponseEntity<PageResponse<CollectionSummaryResponse>> getAllCollections(
      CollectionType type, Pageable pageable) {
    return ResponseEntity.ok(allCollectionsService.execute(type, pageable));
  }

  @Override
  public ResponseEntity<CollectionDetailsResponse> getCollectionDetails(String slug) {
    return ResponseEntity.ok(collectionDetailsService.execute(slug));
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
  public ResponseEntity<SupportTicketCreatedResponse> createSupportTicket(
      CreateSupportTicketRequest request, Authentication authentication) {
    return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
        .body(supportTicketService.execute(request, authentication));
  }
}

package com.gii.api.controller;

import com.gii.api.model.request.admin.AssignInstructorToCourseRequest;
import com.gii.api.model.request.admin.CreateCategoryRequest;
import com.gii.api.model.request.admin.CreateCollectionRequest;
import com.gii.api.model.request.admin.CreateCourseRequest;
import com.gii.api.model.request.admin.CreateInstructorRequest;
import com.gii.api.model.request.admin.CreateLessonResourceRequest;
import com.gii.api.model.request.admin.CreateLessonResourceUploadRequest;
import com.gii.api.model.request.admin.CreateMediaAssetRequest;
import com.gii.api.model.request.admin.CreateQuizRequest;
import com.gii.api.model.request.admin.CreateSectionRequest;
import com.gii.api.model.request.admin.CreateThumbnailUploadRequest;
import com.gii.api.model.request.admin.FeatureCourseRequest;
import com.gii.api.model.request.admin.ReorderCourseStructureRequest;
import com.gii.api.model.request.admin.SetCollectionCoursesRequest;
import com.gii.api.model.request.admin.UpdateCategoryRequest;
import com.gii.api.model.request.admin.UpdateCollectionRequest;
import com.gii.api.model.request.admin.UpdateCourseRequest;
import com.gii.api.model.request.admin.UpdateInstructorRequest;
import com.gii.api.model.request.admin.UpdateLessonResourceRequest;
import com.gii.api.model.request.admin.UpdateMediaAssetRequest;
import com.gii.api.model.request.admin.UpdateOrderRequest;
import com.gii.api.model.request.admin.UpdateQuizRequest;
import com.gii.api.model.request.admin.UpdateSectionRequest;
import com.gii.api.model.request.admin.UpdateSupportTicketRequest;
import com.gii.api.model.request.admin.UpsertAppSettingRequest;
import com.gii.api.model.request.lesson.CreateLessonRequest;
import com.gii.api.model.request.lesson.UpdateLessonRequest;
import com.gii.api.model.response.admin.AdminAppSettingResponse;
import com.gii.api.model.response.admin.AdminCategoryResponse;
import com.gii.api.model.response.admin.AdminCollectionDetailResponse;
import com.gii.api.model.response.admin.AdminCollectionSummaryResponse;
import com.gii.api.model.response.admin.AdminCourseDetailResponse;
import com.gii.api.model.response.admin.AdminCourseReviewResponse;
import com.gii.api.model.response.admin.AdminCourseSectionResponse;
import com.gii.api.model.response.admin.AdminCourseSummaryResponse;
import com.gii.api.model.response.admin.AdminInstructorDetailResponse;
import com.gii.api.model.response.admin.AdminInstructorSummaryResponse;
import com.gii.api.model.response.admin.AdminLessonDetailResponse;
import com.gii.api.model.response.admin.AdminLessonResourceResponse;
import com.gii.api.model.response.admin.AdminLiveClassSummaryResponse;
import com.gii.api.model.response.admin.AdminMediaAssetResponse;
import com.gii.api.model.response.admin.AdminOrderDetailResponse;
import com.gii.api.model.response.admin.AdminOrderSummaryResponse;
import com.gii.api.model.response.admin.AdminQuizDetailResponse;
import com.gii.api.model.response.admin.AdminSupportTicketResponse;
import com.gii.api.model.response.admin.LessonResourceUploadResponse;
import com.gii.api.model.response.admin.ThumbnailUploadResponse;
import com.gii.api.model.response.lesson.ResourceDownloadUrlResponse;
import com.gii.api.service.admin.AdminAppSettingManagementService;
import com.gii.api.service.admin.AdminCategoryManagementService;
import com.gii.api.service.admin.AdminCollectionManagementService;
import com.gii.api.service.admin.AdminCourseManagementService;
import com.gii.api.service.admin.AdminCourseReviewManagementService;
import com.gii.api.service.admin.AdminInstructorManagementService;
import com.gii.api.service.admin.AdminLessonManagementService;
import com.gii.api.service.admin.AdminLessonResourceManagementService;
import com.gii.api.service.admin.AdminLiveClassManagementService;
import com.gii.api.service.admin.AdminMediaAssetManagementService;
import com.gii.api.service.admin.AdminOrderManagementService;
import com.gii.api.service.admin.AdminQuizManagementService;
import com.gii.api.service.admin.AdminSectionManagementService;
import com.gii.api.service.admin.AdminSupportTicketManagementService;
import com.gii.api.service.admin.AdminThumbnailUploadService;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.enums.ReviewStatus;
import com.gii.common.enums.SupportTicketStatus;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminApiController implements AdminApi {

  private final AdminCourseManagementService courseManagementService;
  private final AdminCategoryManagementService categoryManagementService;
  private final AdminCourseReviewManagementService courseReviewManagementService;
  private final AdminSupportTicketManagementService supportTicketManagementService;
  private final AdminCollectionManagementService collectionManagementService;
  private final AdminSectionManagementService sectionManagementService;
  private final AdminLessonManagementService lessonManagementService;
  private final AdminLessonResourceManagementService lessonResourceManagementService;
  private final AdminMediaAssetManagementService mediaAssetManagementService;
  private final AdminQuizManagementService quizManagementService;
  private final AdminInstructorManagementService instructorManagementService;
  private final AdminLiveClassManagementService liveClassManagementService;
  private final AdminOrderManagementService orderManagementService;
  private final AdminThumbnailUploadService thumbnailUploadService;
  private final AdminAppSettingManagementService appSettingManagementService;

  @Override
  public ResponseEntity<ThumbnailUploadResponse> createThumbnailUpload(
      CreateThumbnailUploadRequest request) {
    return ResponseEntity.ok(thumbnailUploadService.execute(request));
  }

  @Override
  public ResponseEntity<List<AdminCategoryResponse>> listCategories() {
    return ResponseEntity.ok(categoryManagementService.list());
  }

  @Override
  public ResponseEntity<AdminCategoryResponse> createCategory(CreateCategoryRequest request) {
    return ResponseEntity.ok(categoryManagementService.create(request));
  }

  @Override
  public ResponseEntity<AdminCategoryResponse> updateCategory(
      UUID categoryId, UpdateCategoryRequest request) {
    return ResponseEntity.ok(categoryManagementService.update(categoryId, request));
  }

  @Override
  public ResponseEntity<List<AdminCourseReviewResponse>> listCourseReviews(ReviewStatus status) {
    return ResponseEntity.ok(courseReviewManagementService.list(status));
  }

  @Override
  public ResponseEntity<Void> publishCourseReview(UUID reviewId) {
    courseReviewManagementService.publish(reviewId);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> unpublishCourseReview(UUID reviewId) {
    courseReviewManagementService.unpublish(reviewId);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> deleteCourseReview(UUID reviewId) {
    courseReviewManagementService.delete(reviewId);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<List<AdminSupportTicketResponse>> listSupportTickets(
      SupportTicketStatus status) {
    return ResponseEntity.ok(supportTicketManagementService.list(status));
  }

  @Override
  public ResponseEntity<AdminSupportTicketResponse> getSupportTicket(UUID ticketId) {
    return ResponseEntity.ok(supportTicketManagementService.get(ticketId));
  }

  @Override
  public ResponseEntity<AdminSupportTicketResponse> updateSupportTicket(
      UUID ticketId, UpdateSupportTicketRequest request) {
    return ResponseEntity.ok(supportTicketManagementService.update(ticketId, request));
  }

  @Override
  public ResponseEntity<List<AdminCollectionSummaryResponse>> listCollections() {
    return ResponseEntity.ok(collectionManagementService.list());
  }

  @Override
  public ResponseEntity<AdminCollectionDetailResponse> createCollection(
      CreateCollectionRequest request, Authentication authentication) {
    return ResponseEntity.ok(collectionManagementService.create(request, authentication));
  }

  @Override
  public ResponseEntity<AdminCollectionDetailResponse> getCollection(UUID collectionId) {
    return ResponseEntity.ok(collectionManagementService.get(collectionId));
  }

  @Override
  public ResponseEntity<AdminCollectionDetailResponse> updateCollection(
      UUID collectionId, UpdateCollectionRequest request) {
    return ResponseEntity.ok(collectionManagementService.update(collectionId, request));
  }

  @Override
  public ResponseEntity<Void> publishCollection(UUID collectionId) {
    collectionManagementService.publish(collectionId);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> unpublishCollection(UUID collectionId) {
    collectionManagementService.unpublish(collectionId);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<AdminCollectionDetailResponse> setCollectionCourses(
      UUID collectionId, SetCollectionCoursesRequest request) {
    return ResponseEntity.ok(collectionManagementService.setCourses(collectionId, request));
  }

  @Override
  public ResponseEntity<List<AdminCourseSummaryResponse>> listCourses() {
    return ResponseEntity.ok(courseManagementService.list());
  }

  @Override
  public ResponseEntity<AdminCourseDetailResponse> createCourse(
      CreateCourseRequest request, Authentication authentication) {
    return ResponseEntity.ok(courseManagementService.create(request, authentication));
  }

  @Override
  public ResponseEntity<AdminCourseDetailResponse> getCourse(UUID courseId) {
    return ResponseEntity.ok(courseManagementService.get(courseId));
  }

  @Override
  public ResponseEntity<AdminCourseDetailResponse> updateCourse(
      UUID courseId, UpdateCourseRequest request) {
    return ResponseEntity.ok(courseManagementService.update(courseId, request));
  }

  @Override
  public ResponseEntity<Void> publishCourse(UUID courseId) {
    courseManagementService.publish(courseId);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> unpublishCourse(UUID courseId) {
    courseManagementService.unpublish(courseId);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<AdminCourseDetailResponse> featureCourse(
      UUID courseId, FeatureCourseRequest request) {
    return ResponseEntity.ok(courseManagementService.feature(courseId, request));
  }

  @Override
  public ResponseEntity<Void> unfeatureCourse(UUID courseId) {
    courseManagementService.unfeature(courseId);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<List<AdminAppSettingResponse>> listSettings() {
    return ResponseEntity.ok(appSettingManagementService.list());
  }

  @Override
  public ResponseEntity<AdminAppSettingResponse> getSetting(String key) {
    return ResponseEntity.ok(appSettingManagementService.get(key));
  }

  @Override
  public ResponseEntity<AdminAppSettingResponse> upsertSetting(
      String key, UpsertAppSettingRequest request) {
    return ResponseEntity.ok(appSettingManagementService.upsert(key, request));
  }

  @Override
  public ResponseEntity<Void> deleteSetting(String key) {
    appSettingManagementService.delete(key);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<AdminCourseSectionResponse> createSection(
      UUID courseId, CreateSectionRequest request) {
    return ResponseEntity.ok(sectionManagementService.create(courseId, request));
  }

  @Override
  public ResponseEntity<AdminCourseSectionResponse> updateSection(
      UUID sectionId, UpdateSectionRequest request) {
    return ResponseEntity.ok(sectionManagementService.update(sectionId, request));
  }

  @Override
  public ResponseEntity<Void> publishSection(UUID sectionId) {
    sectionManagementService.publish(sectionId);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> unpublishSection(UUID sectionId) {
    sectionManagementService.unpublish(sectionId);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> deleteSection(UUID sectionId) {
    sectionManagementService.delete(sectionId);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<AdminLessonDetailResponse> createLesson(
      UUID sectionId, CreateLessonRequest request) {
    return ResponseEntity.ok(lessonManagementService.create(sectionId, request));
  }

  @Override
  public ResponseEntity<AdminLessonDetailResponse> getLesson(UUID lessonId) {
    return ResponseEntity.ok(lessonManagementService.get(lessonId));
  }

  @Override
  public ResponseEntity<AdminLessonDetailResponse> updateLesson(
      UUID lessonId, UpdateLessonRequest request) {
    return ResponseEntity.ok(lessonManagementService.update(lessonId, request));
  }

  @Override
  public ResponseEntity<Void> publishLesson(UUID lessonId) {
    lessonManagementService.publish(lessonId);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> unpublishLesson(UUID lessonId) {
    lessonManagementService.unpublish(lessonId);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> deleteLesson(UUID lessonId) {
    lessonManagementService.delete(lessonId);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<LessonResourceUploadResponse> createLessonResourceUpload(
      UUID lessonId, CreateLessonResourceUploadRequest request) {
    return ResponseEntity.ok(lessonResourceManagementService.createUpload(lessonId, request));
  }

  @Override
  public ResponseEntity<AdminLessonResourceResponse> createLessonResource(
      UUID lessonId, CreateLessonResourceRequest request) {
    return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
        .body(lessonResourceManagementService.create(lessonId, request));
  }

  @Override
  public ResponseEntity<ResourceDownloadUrlResponse> getLessonResourceDownloadUrl(UUID resourceId) {
    return ResponseEntity.ok(lessonResourceManagementService.download(resourceId));
  }

  @Override
  public ResponseEntity<AdminLessonResourceResponse> updateLessonResource(
      UUID resourceId, UpdateLessonResourceRequest request) {
    return ResponseEntity.ok(lessonResourceManagementService.update(resourceId, request));
  }

  @Override
  public ResponseEntity<Void> deleteLessonResource(UUID resourceId) {
    lessonResourceManagementService.delete(resourceId);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> reorderCourseStructure(
      UUID courseId, ReorderCourseStructureRequest request) {
    courseManagementService.reorder(courseId, request);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<AdminMediaAssetResponse> createMediaAsset(CreateMediaAssetRequest request) {
    return ResponseEntity.ok(mediaAssetManagementService.create(request));
  }

  @Override
  public ResponseEntity<AdminMediaAssetResponse> updateMediaAsset(
      UUID mediaAssetId, UpdateMediaAssetRequest request) {
    return ResponseEntity.ok(mediaAssetManagementService.update(mediaAssetId, request));
  }

  @Override
  public ResponseEntity<List<AdminInstructorSummaryResponse>> listInstructors() {
    return ResponseEntity.ok(instructorManagementService.list());
  }

  @Override
  public ResponseEntity<AdminInstructorDetailResponse> createInstructor(
      CreateInstructorRequest request) {
    return ResponseEntity.ok(instructorManagementService.create(request));
  }

  @Override
  public ResponseEntity<AdminInstructorDetailResponse> updateInstructor(
      UUID instructorId, UpdateInstructorRequest request) {
    return ResponseEntity.ok(instructorManagementService.update(instructorId, request));
  }

  @Override
  public ResponseEntity<Void> deleteInstructor(UUID instructorId) {
    instructorManagementService.delete(instructorId);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> assignInstructorToCourse(
      UUID courseId, AssignInstructorToCourseRequest request) {
    instructorManagementService.assign(courseId, request);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Page<AdminLiveClassSummaryResponse>> listLiveClasses(
      int page, int size, List<LiveClassStatus> status) {
    return ResponseEntity.ok(liveClassManagementService.list(page, size, status));
  }

  @Override
  public ResponseEntity<AdminQuizDetailResponse> createQuiz(
      UUID sectionId, CreateQuizRequest request) {
    return ResponseEntity.ok(quizManagementService.create(sectionId, request));
  }

  @Override
  public ResponseEntity<AdminQuizDetailResponse> updateQuiz(
      UUID quizId, UpdateQuizRequest request) {
    return ResponseEntity.ok(quizManagementService.update(quizId, request));
  }

  @Override
  public ResponseEntity<Void> publishQuiz(UUID quizId) {
    quizManagementService.publish(quizId);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> unpublishQuiz(UUID quizId) {
    quizManagementService.unpublish(quizId);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> deleteQuiz(UUID quizId) {
    quizManagementService.delete(quizId);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<List<AdminOrderSummaryResponse>> listOrders() {
    return ResponseEntity.ok(orderManagementService.list());
  }

  @Override
  public ResponseEntity<AdminOrderDetailResponse> getOrder(UUID orderId) {
    return ResponseEntity.ok(orderManagementService.get(orderId));
  }

  @Override
  public ResponseEntity<AdminOrderDetailResponse> updateOrder(
      UUID orderId, UpdateOrderRequest request) {
    return ResponseEntity.ok(orderManagementService.update(orderId, request));
  }
}

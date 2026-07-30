package com.gii.api.controller;

import com.gii.api.model.request.admin.AssignInstructorToCourseRequest;
import com.gii.api.model.request.admin.CreateCollectionRequest;
import com.gii.api.model.request.admin.CreateCourseRequest;
import com.gii.api.model.request.admin.CreateInstructorRequest;
import com.gii.api.model.request.admin.CreateMediaAssetRequest;
import com.gii.api.model.request.admin.CreateQuizRequest;
import com.gii.api.model.request.admin.CreateSectionRequest;
import com.gii.api.model.request.admin.ReorderCourseStructureRequest;
import com.gii.api.model.request.admin.SetCollectionCoursesRequest;
import com.gii.api.model.request.admin.UpdateCourseRequest;
import com.gii.api.model.request.admin.UpdateCollectionRequest;
import com.gii.api.model.request.admin.UpdateInstructorRequest;
import com.gii.api.model.request.admin.UpdateMediaAssetRequest;
import com.gii.api.model.request.admin.UpdateOrderRequest;
import com.gii.api.model.request.admin.UpdateQuizRequest;
import com.gii.api.model.request.admin.UpdateSectionRequest;
import com.gii.api.model.request.lesson.CreateLessonRequest;
import com.gii.api.model.request.lesson.UpdateLessonRequest;
import com.gii.api.model.response.admin.AdminCollectionDetailResponse;
import com.gii.api.model.response.admin.AdminCollectionSummaryResponse;
import com.gii.api.model.response.admin.AdminCourseDetailResponse;
import com.gii.api.model.response.admin.AdminCourseSectionResponse;
import com.gii.api.model.response.admin.AdminCourseSummaryResponse;
import com.gii.api.model.response.admin.AdminInstructorDetailResponse;
import com.gii.api.model.response.admin.AdminInstructorSummaryResponse;
import com.gii.api.model.response.admin.AdminLessonDetailResponse;
import com.gii.api.model.response.admin.AdminLiveClassSummaryResponse;
import com.gii.api.model.response.admin.AdminMediaAssetResponse;
import com.gii.api.model.response.admin.AdminOrderDetailResponse;
import com.gii.api.model.response.admin.AdminOrderSummaryResponse;
import com.gii.api.model.response.admin.AdminQuizDetailResponse;
import com.gii.api.service.admin.AdminCourseManagementService;
import com.gii.api.service.admin.AdminCollectionManagementService;
import com.gii.api.service.admin.AdminInstructorManagementService;
import com.gii.api.service.admin.AdminLessonManagementService;
import com.gii.api.service.admin.AdminLiveClassManagementService;
import com.gii.api.service.admin.AdminMediaAssetManagementService;
import com.gii.api.service.admin.AdminOrderManagementService;
import com.gii.api.service.admin.AdminQuizManagementService;
import com.gii.api.service.admin.AdminSectionManagementService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminApiController implements AdminApi {

  private final AdminCourseManagementService courseManagementService;
  private final AdminCollectionManagementService collectionManagementService;
  private final AdminSectionManagementService sectionManagementService;
  private final AdminLessonManagementService lessonManagementService;
  private final AdminMediaAssetManagementService mediaAssetManagementService;
  private final AdminQuizManagementService quizManagementService;
  private final AdminInstructorManagementService instructorManagementService;
  private final AdminLiveClassManagementService liveClassManagementService;
  private final AdminOrderManagementService orderManagementService;

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
  public ResponseEntity<Void> assignInstructorToCourse(
      UUID courseId, AssignInstructorToCourseRequest request) {
    instructorManagementService.assign(courseId, request);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<List<AdminLiveClassSummaryResponse>> listLiveClasses() {
    return ResponseEntity.ok(liveClassManagementService.list());
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

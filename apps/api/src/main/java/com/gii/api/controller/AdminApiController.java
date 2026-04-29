package com.gii.api.controller;

import com.gii.api.model.request.admin.*;
import com.gii.api.model.request.lesson.CreateLessonRequest;
import com.gii.api.model.request.lesson.UpdateLessonRequest;
import com.gii.api.model.response.admin.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AdminApiController implements AdminApi {

    @Override
    public ResponseEntity<List<AdminCourseSummaryResponse>> listCourses() {
        return null;
    }

    @Override
    public ResponseEntity<AdminCourseDetailResponse> createCourse(CreateCourseRequest request, Authentication authentication) {
        return null;
    }

    @Override
    public ResponseEntity<AdminCourseDetailResponse> getCourse(UUID courseId) {
        return null;
    }

    @Override
    public ResponseEntity<AdminCourseDetailResponse> updateCourse(UUID courseId, UpdateCourseRequest request) {
        return null;
    }

    @Override
    public ResponseEntity<Void> publishCourse(UUID courseId) {
        return null;
    }

    @Override
    public ResponseEntity<Void> unpublishCourse(UUID courseId) {
        return null;
    }

    @Override
    public ResponseEntity<AdminCourseSectionResponse> createSection(UUID courseId, CreateSectionRequest request) {
        return null;
    }

    @Override
    public ResponseEntity<AdminCourseSectionResponse> updateSection(UUID sectionId, UpdateSectionRequest request) {
        return null;
    }

    @Override
    public ResponseEntity<Void> deleteSection(UUID sectionId) {
        return null;
    }

    @Override
    public ResponseEntity<AdminLessonDetailResponse> createLesson(UUID sectionId, CreateLessonRequest request) {
        return null;
    }

    @Override
    public ResponseEntity<AdminLessonDetailResponse> updateLesson(UUID lessonId, UpdateLessonRequest request) {
        return null;
    }

    @Override
    public ResponseEntity<Void> deleteLesson(UUID lessonId) {
        return null;
    }

    @Override
    public ResponseEntity<Void> reorderCourseStructure(UUID courseId, ReorderCourseStructureRequest request) {
        return null;
    }

    @Override
    public ResponseEntity<AdminMediaAssetResponse> createMediaAsset(CreateMediaAssetRequest request) {
        return null;
    }

    @Override
    public ResponseEntity<AdminMediaAssetResponse> updateMediaAsset(UUID mediaAssetId, UpdateMediaAssetRequest request) {
        return null;
    }

    @Override
    public ResponseEntity<List<AdminInstructorSummaryResponse>> listInstructors() {
        return null;
    }

    @Override
    public ResponseEntity<AdminInstructorDetailResponse> createInstructor(CreateInstructorRequest request) {
        return null;
    }

    @Override
    public ResponseEntity<AdminInstructorDetailResponse> updateInstructor(UUID instructorId, UpdateInstructorRequest request) {
        return null;
    }

    @Override
    public ResponseEntity<Void> assignInstructorToCourse(UUID courseId, AssignInstructorToCourseRequest request) {
        return null;
    }

    @Override
    public ResponseEntity<List<AdminLiveClassSummaryResponse>> listLiveClasses() {
        return null;
    }

    @Override
    public ResponseEntity<AdminLiveClassDetailResponse> createLiveClass(UUID courseId, CreateLiveClassRequest request) {
        return null;
    }

    @Override
    public ResponseEntity<AdminLiveClassDetailResponse> updateLiveClass(UUID liveClassId, UpdateLiveClassRequest request) {
        return null;
    }

    @Override
    public ResponseEntity<AdminLiveClassStartResponse> startLiveClass(UUID liveClassId) {
        return null;
    }

    @Override
    public ResponseEntity<AdminQuizDetailResponse> createQuiz(UUID courseId, CreateQuizRequest request) {
        return null;
    }

    @Override
    public ResponseEntity<AdminQuizDetailResponse> updateQuiz(UUID quizId, UpdateQuizRequest request) {
        return null;
    }

    @Override
    public ResponseEntity<Void> publishQuiz(UUID quizId) {
        return null;
    }

    @Override
    public ResponseEntity<List<AdminOrderSummaryResponse>> listOrders() {
        return null;
    }

    @Override
    public ResponseEntity<AdminOrderDetailResponse> getOrder(UUID orderId) {
        return null;
    }

    @Override
    public ResponseEntity<AdminOrderDetailResponse> updateOrder(UUID orderId, UpdateOrderRequest request) {
        return null;
    }
}

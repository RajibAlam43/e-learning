package com.gii.api.controller;

import com.gii.api.model.request.admin.AssignInstructorToCourseRequest;
import com.gii.api.model.request.admin.CreateCourseRequest;
import com.gii.api.model.request.admin.CreateInstructorRequest;
import com.gii.api.model.request.admin.CreateLiveClassRequest;
import com.gii.api.model.request.admin.CreateMediaAssetRequest;
import com.gii.api.model.request.admin.CreateQuizRequest;
import com.gii.api.model.request.admin.CreateSectionRequest;
import com.gii.api.model.request.admin.ReorderCourseStructureRequest;
import com.gii.api.model.request.admin.UpdateCourseRequest;
import com.gii.api.model.request.admin.UpdateInstructorRequest;
import com.gii.api.model.request.admin.UpdateLiveClassRequest;
import com.gii.api.model.request.admin.UpdateMediaAssetRequest;
import com.gii.api.model.request.admin.UpdateOrderRequest;
import com.gii.api.model.request.admin.UpdateQuizRequest;
import com.gii.api.model.request.admin.UpdateSectionRequest;
import com.gii.api.model.request.lesson.CreateLessonRequest;
import com.gii.api.model.request.lesson.UpdateLessonRequest;
import com.gii.api.model.response.admin.AdminCourseDetailResponse;
import com.gii.api.model.response.admin.AdminCourseSectionResponse;
import com.gii.api.model.response.admin.AdminCourseSummaryResponse;
import com.gii.api.model.response.admin.AdminInstructorDetailResponse;
import com.gii.api.model.response.admin.AdminInstructorSummaryResponse;
import com.gii.api.model.response.admin.AdminLessonDetailResponse;
import com.gii.api.model.response.admin.AdminLiveClassDetailResponse;
import com.gii.api.model.response.admin.AdminLiveClassStartResponse;
import com.gii.api.model.response.admin.AdminLiveClassSummaryResponse;
import com.gii.api.model.response.admin.AdminMediaAssetResponse;
import com.gii.api.model.response.admin.AdminOrderDetailResponse;
import com.gii.api.model.response.admin.AdminOrderSummaryResponse;
import com.gii.api.model.response.admin.AdminQuizDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Admin", description = "Admin course, instructor, quiz, and order management")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')") // Ensure only users with ADMIN role can access these endpoints
public interface AdminApi {

  // ===== COURSE MANAGEMENT =====
  @GetMapping("/courses")
  @Operation(summary = "List all courses")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Courses retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
      })
  ResponseEntity<List<AdminCourseSummaryResponse>> listCourses();

  @PostMapping("/courses")
  @Operation(summary = "Create new course")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Course created",
            content = @Content(schema = @Schema(implementation = AdminCourseDetailResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid course data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  ResponseEntity<AdminCourseDetailResponse> createCourse(
      @RequestBody @Valid CreateCourseRequest request, Authentication authentication);

  @GetMapping("/courses/{courseId}")
  @Operation(summary = "Get full course details")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Course retrieved",
            content = @Content(schema = @Schema(implementation = AdminCourseDetailResponse.class))),
        @ApiResponse(responseCode = "404", description = "Course not found")
      })
  ResponseEntity<AdminCourseDetailResponse> getCourse(@PathVariable UUID courseId);

  @PatchMapping("/courses/{courseId}")
  @Operation(summary = "Update course")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Course updated",
            content = @Content(schema = @Schema(implementation = AdminCourseDetailResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Course not found")
      })
  ResponseEntity<AdminCourseDetailResponse> updateCourse(
      @PathVariable UUID courseId, @Valid @RequestBody UpdateCourseRequest request);

  @PostMapping("/courses/{courseId}/publish")
  @Operation(summary = "Publish course")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Course published"),
        @ApiResponse(
            responseCode = "400",
            description = "Cannot publish - missing required fields"),
        @ApiResponse(responseCode = "404", description = "Course not found")
      })
  ResponseEntity<Void> publishCourse(@PathVariable UUID courseId);

  @PostMapping("/courses/{courseId}/unpublish")
  @Operation(summary = "Unpublish course")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Course unpublished"),
        @ApiResponse(responseCode = "404", description = "Course not found")
      })
  ResponseEntity<Void> unpublishCourse(@PathVariable UUID courseId);

  // ===== SECTION MANAGEMENT =====
  @PostMapping("/courses/{courseId}/sections")
  @Operation(summary = "Create course section")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Section created"),
        @ApiResponse(responseCode = "404", description = "Course not found")
      })
  ResponseEntity<AdminCourseSectionResponse> createSection(
      @PathVariable UUID courseId, @Valid @RequestBody CreateSectionRequest request);

  @PatchMapping("/sections/{sectionId}")
  @Operation(summary = "Update section")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Section updated"),
        @ApiResponse(responseCode = "404", description = "Section not found")
      })
  ResponseEntity<AdminCourseSectionResponse> updateSection(
      @PathVariable UUID sectionId, @Valid @RequestBody UpdateSectionRequest request);

  @DeleteMapping("/sections/{sectionId}")
  @Operation(summary = "Delete section")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Section deleted"),
        @ApiResponse(responseCode = "404", description = "Section not found")
      })
  ResponseEntity<Void> deleteSection(@PathVariable UUID sectionId);

  // ===== LESSON MANAGEMENT =====
  @PostMapping("/sections/{sectionId}/lessons")
  @Operation(summary = "Create lesson")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Lesson created"),
        @ApiResponse(responseCode = "404", description = "Section not found")
      })
  ResponseEntity<AdminLessonDetailResponse> createLesson(
      @PathVariable UUID sectionId, @Valid @RequestBody CreateLessonRequest request);

  @PatchMapping("/lessons/{lessonId}")
  @Operation(summary = "Update lesson")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Lesson updated"),
        @ApiResponse(responseCode = "404", description = "Lesson not found")
      })
  ResponseEntity<AdminLessonDetailResponse> updateLesson(
      @PathVariable UUID lessonId, @Valid @RequestBody UpdateLessonRequest request);

  @DeleteMapping("/lessons/{lessonId}")
  @Operation(summary = "Delete lesson")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Lesson deleted"),
        @ApiResponse(responseCode = "404", description = "Lesson not found")
      })
  ResponseEntity<Void> deleteLesson(@PathVariable UUID lessonId);

  @PostMapping("/courses/{courseId}/structure/reorder")
  @Operation(summary = "Reorder course structure")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Structure reordered"),
        @ApiResponse(responseCode = "400", description = "Invalid reorder data")
      })
  ResponseEntity<Void> reorderCourseStructure(
      @PathVariable UUID courseId, @Valid @RequestBody ReorderCourseStructureRequest request);

  // ===== MEDIA ASSET MANAGEMENT =====
  @PostMapping("/media-assets")
  @Operation(summary = "Create media asset")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Media asset created"),
        @ApiResponse(responseCode = "404", description = "Lesson not found")
      })
  ResponseEntity<AdminMediaAssetResponse> createMediaAsset(
      @Valid @RequestBody CreateMediaAssetRequest request);

  @PatchMapping("/media-assets/{mediaAssetId}")
  @Operation(summary = "Update media asset")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Media asset updated",
            content = @Content(schema = @Schema(implementation = AdminMediaAssetResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Media asset not found")
      })
  ResponseEntity<AdminMediaAssetResponse> updateMediaAsset(
      @PathVariable UUID mediaAssetId, @RequestBody UpdateMediaAssetRequest request);

  // ===== INSTRUCTOR MANAGEMENT =====
  @GetMapping("/instructors")
  @Operation(summary = "List instructors")
  ResponseEntity<List<AdminInstructorSummaryResponse>> listInstructors();

  @PostMapping("/instructors")
  @Operation(summary = "Create instructor")
  ResponseEntity<AdminInstructorDetailResponse> createInstructor(
      @Valid @RequestBody CreateInstructorRequest request);

  @PatchMapping("/instructors/{instructorId}")
  @Operation(summary = "Update instructor")
  ResponseEntity<AdminInstructorDetailResponse> updateInstructor(
      @PathVariable UUID instructorId, @Valid @RequestBody UpdateInstructorRequest request);

  @PostMapping("/courses/{courseId}/instructors")
  @Operation(summary = "Assign instructor to course")
  ResponseEntity<Void> assignInstructorToCourse(
      @PathVariable UUID courseId, @Valid @RequestBody AssignInstructorToCourseRequest request);

  // ===== LIVE CLASS MANAGEMENT =====
  @GetMapping("/live-classes")
  @Operation(summary = "List live classes")
  ResponseEntity<List<AdminLiveClassSummaryResponse>> listLiveClasses();

  @PostMapping("/courses/{courseId}/live-classes")
  @Operation(summary = "Create live class")
  ResponseEntity<AdminLiveClassDetailResponse> createLiveClass(
      @PathVariable UUID courseId, @Valid @RequestBody CreateLiveClassRequest request);

  @PatchMapping("/live-classes/{liveClassId}")
  @Operation(summary = "Update live class")
  ResponseEntity<AdminLiveClassDetailResponse> updateLiveClass(
      @PathVariable UUID liveClassId, @Valid @RequestBody UpdateLiveClassRequest request);

  @PostMapping("/live-classes/{liveClassId}/start")
  @Operation(summary = "Start live class")
  ResponseEntity<AdminLiveClassStartResponse> startLiveClass(@PathVariable UUID liveClassId);

  // ===== QUIZ MANAGEMENT =====
  @PostMapping("/sections/{sectionId}/quizzes")
  @Operation(summary = "Create quiz")
  ResponseEntity<AdminQuizDetailResponse> createQuiz(
      @PathVariable UUID sectionId, @Valid @RequestBody CreateQuizRequest request);

  @PatchMapping("/quizzes/{quizId}")
  @Operation(summary = "Update quiz")
  ResponseEntity<AdminQuizDetailResponse> updateQuiz(
      @PathVariable UUID quizId, @Valid @RequestBody UpdateQuizRequest request);

  @PostMapping("/quizzes/{quizId}/publish")
  @Operation(summary = "Publish quiz")
  ResponseEntity<Void> publishQuiz(@PathVariable UUID quizId);

  // ===== ORDER MANAGEMENT =====
  @GetMapping("/orders")
  @Operation(summary = "List orders")
  ResponseEntity<List<AdminOrderSummaryResponse>> listOrders();

  @GetMapping("/orders/{orderId}")
  @Operation(summary = "Get order details")
  ResponseEntity<AdminOrderDetailResponse> getOrder(@PathVariable UUID orderId);

  @PatchMapping("/orders/{orderId}")
  @Operation(summary = "Update order")
  ResponseEntity<AdminOrderDetailResponse> updateOrder(
      @PathVariable UUID orderId, @Valid @RequestBody UpdateOrderRequest request);
}

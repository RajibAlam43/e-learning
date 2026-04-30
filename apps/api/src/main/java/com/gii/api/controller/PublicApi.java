package com.gii.api.controller;

import com.gii.api.model.request.CreateSupportTicketRequest;
import com.gii.api.model.response.CourseDetailsResponse;
import com.gii.api.model.response.CourseSummaryResponse;
import com.gii.api.model.response.PageResponse;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Public", description = "Public course catalog, instructors, and support")
@RequestMapping("/public")
public interface PublicApi {

  @GetMapping("/courses")
  @Operation(
      summary = "List published courses",
      description =
          "Get all published courses with optional filters (category, level, language)"
              + " and pagination.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Courses retrieved",
            content = @Content(schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid filter parameters")
      })
  ResponseEntity<PageResponse<CourseSummaryResponse>> getAllCourses(
      @RequestParam(required = false) UUID categoryId,
      @RequestParam(required = false) CourseLevel level,
      @RequestParam(required = false) CourseLanguage language,
      @PageableDefault(size = 20, sort = "publishedAt") Pageable pageable);

  @GetMapping("/courses/{slug}")
  @Operation(
      summary = "Get course details",
      description =
          "Get detailed public course information: description, sections, lessons preview,"
              + " instructor, pricing.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Course details retrieved",
            content = @Content(schema = @Schema(implementation = CourseDetailsResponse.class))),
        @ApiResponse(responseCode = "404", description = "Course not found")
      })
  ResponseEntity<CourseDetailsResponse> getCourseDetails(@PathVariable String slug);

  /*
  @GetMapping("/instructors")
  @Operation(
          summary = "List published instructors",
          description = "Get all instructors with public profiles."
  )
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Instructors retrieved")
  })
  ResponseEntity<?> getAllInstructors();

  @GetMapping("/instructors/{slug}")
  @Operation(
          summary = "Get instructor details",
          description = "Get detailed instructor profile including courses taught and credentials."
  )
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Instructor details retrieved"),
          @ApiResponse(responseCode = "404", description = "Instructor not found")
  })
  ResponseEntity<?> getInstructorDetails(@PathVariable String slug);
  */

  @PostMapping("/support/tickets")
  @Operation(
      summary = "Create support ticket",
      description = "Submit a support request. Does not require authentication.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Support ticket created"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
      })
  ResponseEntity<Void> createSupportTicket(@RequestBody CreateSupportTicketRequest request);
}

package com.gii.api.controller;

import com.gii.api.model.request.CreateSupportTicketRequest;
import com.gii.api.model.response.CourseDetailsResponse;
import com.gii.api.model.response.CourseSummaryResponse;
import com.gii.api.model.response.PageResponse;
import com.gii.api.service.open.AllCoursesService;
import com.gii.api.service.open.CourseDetailsService;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PublicApiController implements PublicApi {

    private final AllCoursesService allCoursesService;
    private final CourseDetailsService courseDetailsService;

    @Override
    public ResponseEntity<PageResponse<CourseSummaryResponse>> getAllCourses(UUID categoryId, CourseLevel level, CourseLanguage language, Pageable pageable) {
        return ResponseEntity.ok(allCoursesService.execute(categoryId, level, language, pageable));
    }

    @Override
    public ResponseEntity<CourseDetailsResponse> getCourseDetails(String slug) {
        return ResponseEntity.ok(courseDetailsService.execute(slug));
    }

    @Override
    public ResponseEntity<?> getAllInstructors() {
        return null;
    }

    @Override
    public ResponseEntity<?> getInstructorDetails(String slug) {
        return null;
    }

    @Override
    public ResponseEntity<Void> createSupportTicket(CreateSupportTicketRequest request) {
        return null;
    }
}

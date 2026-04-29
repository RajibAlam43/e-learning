package com.gii.api.controller;

import com.gii.api.model.request.CreateSupportTicketRequest;
import com.gii.api.model.response.CourseDetailsResponse;
import com.gii.api.model.response.CourseSummaryResponse;
import com.gii.api.model.response.PageResponse;
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

    @Override
    public ResponseEntity<PageResponse<CourseSummaryResponse>> getAllCourses(UUID categoryId, CourseLevel level, CourseLanguage language, Pageable pageable) {
        return null;
    }

    @Override
    public ResponseEntity<CourseDetailsResponse> getCourseDetails(String slug) {
        return null;
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

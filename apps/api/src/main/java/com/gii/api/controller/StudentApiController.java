package com.gii.api.controller;

import com.gii.api.model.response.student.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class StudentApiController implements StudentApi {

    @Override
    public ResponseEntity<StudentDashboardResponse> getDashboard(Authentication authentication) {
        return null;
    }

    @Override
    public ResponseEntity<List<StudentCourseSummaryResponse>> getMyCourses(Authentication authentication) {
        return null;
    }

    @Override
    public ResponseEntity<StudentCourseHomeResponse> getMyCourseDetails(UUID courseId, Authentication authentication) {
        return null;
    }

    @Override
    public ResponseEntity<List<StudentOrderSummaryResponse>> getMyOrders(Authentication authentication) {
        return null;
    }

    @Override
    public ResponseEntity<List<StudentCertificateSummaryResponse>> getMyCertificates(Authentication authentication) {
        return null;
    }

    @Override
    public ResponseEntity<List<StudentLiveClassSummaryResponse>> getUpcomingLiveClasses(Authentication authentication) {
        return null;
    }

    @Override
    public ResponseEntity<List<StudentLiveClassSummaryResponse>> getCourseLiveClasses(UUID courseId, Authentication authentication) {
        return null;
    }

    @Override
    public ResponseEntity<StudentLiveClassJoinResponse> joinLiveClass(UUID liveClassId, Authentication authentication) {
        return null;
    }
}


package com.gii.api.controller;

import com.gii.api.model.request.instructor.CreateLiveClassRequest;
import com.gii.api.model.request.instructor.UpdateLiveClassRequest;
import com.gii.api.model.response.instructor.InstructorDashboardResponse;
import com.gii.api.model.response.instructor.InstructorLiveClassResponse;
import com.gii.api.model.response.instructor.InstructorLiveClassStartResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class InstructorApiController implements InstructorApi {

    @Override
    public ResponseEntity<InstructorDashboardResponse> getDashboard(Authentication authentication) {
        return null;
    }

    @Override
    public ResponseEntity<InstructorLiveClassResponse> createLiveClass(UUID courseId, CreateLiveClassRequest request, Authentication authentication) {
        return null;
    }

    @Override
    public ResponseEntity<InstructorLiveClassStartResponse> startLiveClass(UUID liveClassId, Authentication authentication) {
        return null;
    }

    @Override
    public ResponseEntity<InstructorLiveClassResponse> updateLiveClass(UUID liveClassId, UpdateLiveClassRequest request, Authentication authentication) {
        return null;
    }

    @Override
    public ResponseEntity<Void> deleteLiveClass(UUID liveClassId, Authentication authentication) {
        return null;
    }
}

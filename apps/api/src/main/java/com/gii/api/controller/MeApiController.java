package com.gii.api.controller;

import com.gii.api.model.request.me.UpdateProfileRequest;
import com.gii.api.model.response.me.MeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MeApiController implements MeApi {

    @Override
    public ResponseEntity<MeResponse> getMe(Authentication authentication) {
        return null;
    }

    @Override
    public ResponseEntity<MeResponse> updateProfile(UpdateProfileRequest request, Authentication authentication) {
        return null;
    }
}

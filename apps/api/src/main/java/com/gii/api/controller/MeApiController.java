package com.gii.api.controller;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/me")
public class MeApiController {

    /**
     * Get current user
     *
     * @return
     */
    @GetMapping
    public ResponseEntity<@NotNull MeResponse> getMe() {
        return ResponseEntity.ok(meApiProcessingService.getCurrentUser());
    }

    /**
     * Update name/profile info
     *
     * @param request
     * @return
     */
    @PatchMapping("/profile")
    public ResponseEntity<@NotNull MeResponse> updateProfile(@RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(meApiProcessingService.updateProfile(request));
    }
}

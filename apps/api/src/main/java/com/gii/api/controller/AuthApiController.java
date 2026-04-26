package com.gii.api.controller;

import com.gii.api.model.request.*;
import com.gii.api.model.response.AuthResponse;
import com.gii.api.processor.AuthApiProcessingService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/public/auth")
public class AuthApiController {

    private final AuthApiProcessingService authApiProcessingService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request, HttpServletResponse response) {
        return ResponseEntity.ok(authApiProcessingService.register(request, response));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        return ResponseEntity.ok(authApiProcessingService.login(request, response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue("refresh_token") String refreshToken, HttpServletResponse response) {
        return ResponseEntity.ok(authApiProcessingService.refresh(refreshToken, response));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authApiProcessingService.forgotPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        authApiProcessingService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/auth/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestBody VerifyEmailRequest request) {
        authApiProcessingService.verifyEmail(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/auth/resend-email-verification")
    public ResponseEntity<Void> resendVerification(@RequestBody ResendVerificationRequest request) {
        authApiProcessingService.resendVerification(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "refresh_token", required = false) String refreshToken, HttpServletResponse response) {
        authApiProcessingService.logout(refreshToken, response);
        return ResponseEntity.ok().build();
    }
}

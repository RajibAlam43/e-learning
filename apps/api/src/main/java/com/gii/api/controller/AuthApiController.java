package com.gii.api.controller;

import com.gii.api.model.request.*;
import com.gii.api.model.response.AuthResponse;
import com.gii.api.processor.AuthApiProcessingService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/public/auth")
public class AuthApiController {

    private final AuthApiProcessingService authApiProcessingService;

    @PostMapping("/register")
    public ResponseEntity<@NotNull AuthResponse> register(@RequestBody RegisterRequest request, HttpServletResponse response) {
        return ResponseEntity.ok(authApiProcessingService.register(request, response));
    }

    @PostMapping("/login")
    public ResponseEntity<@NotNull AuthResponse> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        return ResponseEntity.ok(authApiProcessingService.login(request, response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<@NotNull AuthResponse> refresh(@CookieValue("refresh_token") String refreshToken, HttpServletResponse response) {
        return ResponseEntity.ok(authApiProcessingService.refresh(refreshToken, response));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<@NotNull Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authApiProcessingService.forgotPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<@NotNull Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        authApiProcessingService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/auth/verify-email")
    public ResponseEntity<@NotNull Void> verifyEmail(@RequestBody VerifyEmailRequest request) {
        authApiProcessingService.verifyEmail(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/auth/resend-email-verification")
    public ResponseEntity<@NotNull Void> resendVerification(@RequestBody ResendVerificationRequest request) {
        authApiProcessingService.resendVerification(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<@NotNull Void> logout(@CookieValue(name = "refresh_token", required = false) String refreshToken, HttpServletResponse response) {
        authApiProcessingService.logout(refreshToken, response);
        return ResponseEntity.ok().build();
    }
}

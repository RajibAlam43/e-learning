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

    /**
     * Register with email/phone and password
     *
     * @param request
     * @param response
     * @return
     */
    @PostMapping("/register")
    public ResponseEntity<@NotNull AuthResponse> register(@RequestBody RegisterRequest request, HttpServletResponse response) {
        return ResponseEntity.ok(authApiProcessingService.register(request, response));
    }

    /**
     * Login using email or phone
     *
     * @param request
     * @param response
     * @return
     */
    @PostMapping("/login")
    public ResponseEntity<@NotNull AuthResponse> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        return ResponseEntity.ok(authApiProcessingService.login(request, response));
    }

    /**
     * Refresh access token
     *
     * @param refreshToken
     * @param response
     * @return
     */
    @PostMapping("/refresh")
    public ResponseEntity<@NotNull AuthResponse> refresh(@CookieValue("refresh_token") String refreshToken, HttpServletResponse response) {
        return ResponseEntity.ok(authApiProcessingService.refresh(refreshToken, response));
    }

    /**
     * Send reset code/link to email or phone
     *
     * @param request
     * @return
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<@NotNull Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authApiProcessingService.forgotPassword(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Reset password
     *
     * @param request
     * @return
     */
    @PostMapping("/reset-password")
    public ResponseEntity<@NotNull Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        authApiProcessingService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Send OTP code to email or phone
     *
     * @param request
     * @return
     */
    @PostMapping("verification/send")
    public ResponseEntity<@NotNull Void> sendPhoneCode(@RequestBody SendCodeRequest request) {
        authApiProcessingService.sendVerification(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Verify email or phone code
     *
     * @param request
     * @return
     */
    @PostMapping("verification/verify")
    public ResponseEntity<@NotNull Void> verifyPhoneCode(@RequestBody VerifyCodeRequest request) {
        authApiProcessingService.verify(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Resend code to phone or email
     *
     * @param request
     * @return
     */
    @PostMapping("verification/resend")
    public ResponseEntity<@NotNull Void> resendPhoneCode(@RequestBody ResendCodeRequest request) {
        authApiProcessingService.resendVerification(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Logout
     *
     * @param refreshToken
     * @param response
     * @return
     */
    @PostMapping("/logout")
    public ResponseEntity<@NotNull Void> logout(@CookieValue(name = "refresh_token", required = false) String refreshToken, HttpServletResponse response) {
        authApiProcessingService.logout(refreshToken, response);
        return ResponseEntity.ok().build();
    }
}

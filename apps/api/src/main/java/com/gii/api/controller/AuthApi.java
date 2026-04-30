package com.gii.api.controller;

import com.gii.api.model.request.auth.ForgotPasswordRequest;
import com.gii.api.model.request.auth.LoginRequest;
import com.gii.api.model.request.auth.RegisterRequest;
import com.gii.api.model.request.auth.ResetPasswordRequest;
import com.gii.api.model.request.auth.SendVerificationRequest;
import com.gii.api.model.request.auth.VerifyRequest;
import com.gii.api.model.response.auth.AuthResponse;
import com.gii.api.model.response.auth.RegisterResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(
    name = "Authentication",
    description = "User registration, login, and authentication operations")
@RequestMapping("/public/auth")
public interface AuthApi {

  @PostMapping("/register")
  @Operation(
      summary = "Register a new user",
      description =
          "Register with email/phone and password. At least one of email or phone is required.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Registration successful",
            content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input or user already exists"),
        @ApiResponse(responseCode = "409", description = "Email or phone already registered")
      })
  ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request);

  @PostMapping("/login")
  @Operation(
      summary = "Login user",
      description =
          "Login using email or phone with password. Returns JWT access token and sets refresh"
              + " token in httpOnly cookie.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "404", description = "User not found")
      })
  ResponseEntity<AuthResponse> login(
      @RequestBody LoginRequest request, HttpServletResponse response);

  @PostMapping("/refresh")
  @Operation(
      summary = "Refresh access token",
      description = "Generate a new access token using valid refresh token from httpOnly cookie.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token refreshed",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token"),
        @ApiResponse(responseCode = "403", description = "Refresh token revoked")
      })
  ResponseEntity<AuthResponse> refresh(
      @CookieValue("refresh_token") String refreshToken, HttpServletResponse response);

  @PostMapping("/forgot-password")
  @Operation(
      summary = "Send password reset code",
      description =
          "Send password reset code/link to email or phone. User must verify the code before"
              + " resetting password.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Reset code sent successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid email or phone"),
        @ApiResponse(responseCode = "404", description = "User not found")
      })
  ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequest request);

  @PostMapping("/reset-password")
  @Operation(
      summary = "Reset password using token",
      description = "Reset password by providing valid reset token and new password.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Password reset successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired token"),
        @ApiResponse(responseCode = "422", description = "Password validation failed")
      })
  ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request);

  @PostMapping("/send-code")
  @Operation(
      summary = "Send verification code",
      description = "Send OTP/verification code to email or phone for email or phone verification.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Verification code sent"),
        @ApiResponse(responseCode = "400", description = "Invalid email or phone"),
        @ApiResponse(responseCode = "429", description = "Too many verification attempts")
      })
  ResponseEntity<Void> sendVerification(@RequestBody SendVerificationRequest request);

  @PostMapping("/verify-code")
  @Operation(
      summary = "Verify email or phone",
      description = "Verify email or phone by submitting the OTP/code received.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Email/phone verified successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired code"),
        @ApiResponse(responseCode = "403", description = "Too many failed attempts")
      })
  ResponseEntity<Void> verify(@RequestBody VerifyRequest request);

  @PostMapping("/logout")
  @Operation(
      summary = "Logout user",
      description =
          "Logout by invalidating refresh token. Clear authenticati on cookies on client.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Logout successful"),
        @ApiResponse(responseCode = "401", description = "No active session")
      })
  ResponseEntity<Void> logout(
      @CookieValue(name = "refresh_token", required = false) String refreshToken,
      HttpServletResponse response);
}

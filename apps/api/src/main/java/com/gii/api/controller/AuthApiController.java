package com.gii.api.controller;

import com.gii.api.model.request.auth.ForgotPasswordRequest;
import com.gii.api.model.request.auth.LoginRequest;
import com.gii.api.model.request.auth.RegisterRequest;
import com.gii.api.model.request.auth.ResetPasswordRequest;
import com.gii.api.model.request.auth.SendVerificationRequest;
import com.gii.api.model.request.auth.VerifyRequest;
import com.gii.api.model.response.auth.AuthResponse;
import com.gii.api.model.response.auth.RegisterResponse;
import com.gii.api.service.auth.ForgotPasswordService;
import com.gii.api.service.auth.LoginService;
import com.gii.api.service.auth.LogoutService;
import com.gii.api.service.auth.RefreshService;
import com.gii.api.service.auth.RegisterService;
import com.gii.api.service.auth.ResetPasswordService;
import com.gii.api.service.auth.SendVerificationService;
import com.gii.api.service.auth.VerifyService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthApiController implements AuthApi {

  private final RegisterService registerService;
  private final LoginService loginService;
  private final RefreshService refreshService;
  private final ForgotPasswordService forgotPasswordService;
  private final ResetPasswordService resetPasswordService;
  private final SendVerificationService sendVerificationService;
  private final VerifyService verifyService;
  private final LogoutService logoutService;

  public ResponseEntity<@NotNull RegisterResponse> register(@RequestBody RegisterRequest request) {
    return ResponseEntity.ok(registerService.execute(request));
  }

  public ResponseEntity<@NotNull AuthResponse> login(
      @RequestBody LoginRequest request, HttpServletResponse response) {
    return ResponseEntity.ok(loginService.execute(request, response));
  }

  public ResponseEntity<@NotNull AuthResponse> refresh(
      @CookieValue("refresh_token") String refreshToken, HttpServletResponse response) {
    return ResponseEntity.ok(refreshService.execute(refreshToken, response));
  }

  public ResponseEntity<@NotNull Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
    forgotPasswordService.execute(request);
    return ResponseEntity.ok().build();
  }

  public ResponseEntity<@NotNull Void> resetPassword(@RequestBody ResetPasswordRequest request) {
    resetPasswordService.execute(request);
    return ResponseEntity.ok().build();
  }

  public ResponseEntity<@NotNull Void> sendVerification(
      @RequestBody SendVerificationRequest request) {
    sendVerificationService.execute(request);
    return ResponseEntity.ok().build();
  }

  public ResponseEntity<@NotNull Void> verify(@RequestBody VerifyRequest request) {
    verifyService.execute(request);
    return ResponseEntity.ok().build();
  }

  public ResponseEntity<@NotNull Void> logout(
      @CookieValue(name = "refresh_token", required = false) String refreshToken,
      HttpServletResponse response) {
    logoutService.execute(refreshToken, response);
    return ResponseEntity.ok().build();
  }
}

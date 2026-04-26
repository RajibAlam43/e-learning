package com.gii.api.processor;

import com.gii.api.model.request.*;
import com.gii.api.model.response.AuthResponse;
import com.gii.api.service.auth.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthApiProcessingService {

    private final RegisterService registerService;
    private final LoginService loginService;
    private final RefreshTokenService refreshTokenService;
    private final ForgotPasswordService forgotPasswordService;
    private final ResetPasswordService resetPasswordService;
    private final VerifyEmailService verifyEmailService;
    private final ResendVerificationService resendVerificationService;
    private final LogoutService logoutService;

    public AuthResponse register(RegisterRequest request, HttpServletResponse response) {
        return registerService.execute(request, response);
    }

    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        return loginService.execute(request, response);
    }

    public AuthResponse refresh(String refreshToken, HttpServletResponse response) {
        return refreshTokenService.execute(refreshToken, response);
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        forgotPasswordService.execute(request.email());
    }

    public void resetPassword(ResetPasswordRequest request) {
        resetPasswordService.execute(request.token(), request.newPassword());
    }

    public void verifyEmail(VerifyEmailRequest request) {
        verifyEmailService.execute(request.token());
    }

    public void resendVerification(ResendVerificationRequest request) {
        resendVerificationService.execute(request.email());
    }

    public void logout(String refreshToken, HttpServletResponse response) {
        logoutService.execute(refreshToken, response);
    }
}

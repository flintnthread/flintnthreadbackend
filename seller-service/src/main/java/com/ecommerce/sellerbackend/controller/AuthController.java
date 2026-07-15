package com.ecommerce.sellerbackend.controller;

import com.ecommerce.sellerbackend.dto.EmailVerificationResponse;
import com.ecommerce.sellerbackend.dto.ForgotPasswordRequest;
import com.ecommerce.sellerbackend.dto.LoginRequest;
import com.ecommerce.sellerbackend.dto.LoginResponse;
import com.ecommerce.sellerbackend.dto.MessageResponse;
import com.ecommerce.sellerbackend.dto.RefreshTokenResponse;
import com.ecommerce.sellerbackend.dto.ResendEmailOtpRequest;
import com.ecommerce.sellerbackend.dto.ResetPasswordRequest;
import com.ecommerce.sellerbackend.dto.ResetTokenValidationResponse;
import com.ecommerce.sellerbackend.dto.StartEmailVerificationRequest;
import com.ecommerce.sellerbackend.dto.StartEmailVerificationResponse;
import com.ecommerce.sellerbackend.dto.VerifyEmailOtpRequest;
import com.ecommerce.sellerbackend.service.AuthService;
import com.ecommerce.sellerbackend.service.EmailVerificationService;
import com.ecommerce.sellerbackend.service.PasswordResetService;
import com.ecommerce.sellerbackend.util.EmailVerificationUrlHelper;
import com.ecommerce.sellerbackend.util.PasswordResetUrlHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;
    private final EmailVerificationUrlHelper emailVerificationUrlHelper;
    private final PasswordResetUrlHelper passwordResetUrlHelper;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, resolveClientIp(httpRequest), httpRequest.getHeader("User-Agent"));
    }

    /** Extends session while seller is actively using the app (sliding expiry). */
    @PostMapping("/refresh")
    public RefreshTokenResponse refreshSession(HttpServletRequest httpRequest) {
        return authService.refreshSession(httpRequest.getHeader("Authorization"));
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return passwordResetService.requestPasswordReset(request);
    }

    @GetMapping("/reset-password/validate")
    public ResetTokenValidationResponse validateResetToken(@RequestParam String token) {
        return passwordResetService.validateResetToken(token);
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return passwordResetService.resetPassword(request);
    }

    /**
     * User clicks the reset link in the forgot-password email.
     * Validates the token and redirects to the reset-password page.
     */
    @GetMapping("/reset-password")
    public void resetPasswordFromLink(
            @RequestParam("token") String token,
            HttpServletResponse response) throws IOException {
        String redirectUrl;
        try {
            ResetTokenValidationResponse validation = passwordResetService.validateResetToken(token);
            if (validation.isValid()) {
                redirectUrl = passwordResetUrlHelper.buildResetPageRedirect(token);
            } else {
                redirectUrl = passwordResetUrlHelper.buildResetPageRedirectError(validation.getMessage());
            }
        } catch (Exception ex) {
            redirectUrl = passwordResetUrlHelper.buildResetPageRedirectError(
                    "This reset link is invalid or has expired. Please request a new one.");
        }
        response.sendRedirect(redirectUrl);
    }

    /**
     * Step 2 of email verification (web + mobile browser).
     * User clicks the link in the signup/admin verification email;
     * backend sends the 6-digit OTP and redirects to the OTP entry page.
     */
    @GetMapping("/verify-email")
    public void verifyEmailFromLink(
            @RequestParam("token") String token,
            HttpServletResponse response) throws IOException {
        String redirectUrl;
        try {
            StartEmailVerificationRequest request = new StartEmailVerificationRequest();
            request.setToken(token);
            StartEmailVerificationResponse result = emailVerificationService.confirmEmailLink(request);
            redirectUrl = emailVerificationUrlHelper.buildOtpPageRedirect(
                    result.getEmail(),
                    result.isOtpSent(),
                    result.isAlreadyVerified()
            );
        } catch (IllegalArgumentException ex) {
            redirectUrl = emailVerificationUrlHelper.buildOtpPageRedirectError(ex.getMessage());
        }
        response.sendRedirect(redirectUrl);
    }

    /** Same as GET /verify-email; used by mobile app / SPA when the link opens the app with a token. */
    @PostMapping("/confirm-email-link")
    public StartEmailVerificationResponse confirmEmailLink(
            @Valid @RequestBody StartEmailVerificationRequest request) {
        return emailVerificationService.confirmEmailLink(request);
    }

    @PostMapping("/verify-email-otp")
    public EmailVerificationResponse verifyEmailOtp(@Valid @RequestBody VerifyEmailOtpRequest request) {
        return emailVerificationService.verifyEmailOtp(request);
    }

    @PostMapping("/resend-email-otp")
    public MessageResponse resendEmailOtp(@Valid @RequestBody ResendEmailOtpRequest request) {
        return emailVerificationService.resendEmailOtp(request);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

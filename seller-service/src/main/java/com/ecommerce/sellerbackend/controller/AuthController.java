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
     * One-click email verification from the signup mail.
     * Verifies on seller-service (no Expo/frontend required) and shows a success page.
     */
    @GetMapping("/verify-email")
    public void verifyEmailFromLink(
            @RequestParam("token") String token,
            HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        try {
            EmailVerificationResponse result = emailVerificationService.verifyEmailFromLinkToken(token);
            String loginUrl = emailVerificationUrlHelper.buildLoginPageUrl(result.getEmail());
            writeVerificationHtml(
                    response,
                    HttpServletResponse.SC_OK,
                    "Email verified",
                    escapeHtml(result.getMessage()),
                    loginUrl,
                    true
            );
        } catch (IllegalArgumentException ex) {
            writeVerificationHtml(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Verification failed",
                    escapeHtml(ex.getMessage()),
                    emailVerificationUrlHelper.buildLoginPageUrl(null),
                    false
            );
        }
    }

    private static void writeVerificationHtml(
            HttpServletResponse response,
            int status,
            String title,
            String message,
            String loginUrl,
            boolean success) throws IOException {
        response.setStatus(status);
        response.setContentType("text/html;charset=UTF-8");
        String accent = success ? "#16a34a" : "#dc2626";
        String buttonLabel = success ? "Go to Login" : "Back to Login";
        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1"/>
                  <title>%s — Flint &amp; Thread</title>
                  <style>
                    body{margin:0;font-family:Segoe UI,system-ui,sans-serif;background:#f8fafc;color:#0f172a;
                      display:flex;min-height:100vh;align-items:center;justify-content:center;padding:24px}
                    .card{max-width:440px;width:100%%;background:#fff;border-radius:16px;padding:32px 28px;
                      box-shadow:0 10px 40px rgba(15,23,42,.08);text-align:center}
                    h1{margin:0 0 12px;font-size:1.35rem;color:%s}
                    p{margin:0 0 24px;line-height:1.5;color:#475569}
                    a{display:inline-block;background:#F97316;color:#fff;text-decoration:none;
                      padding:12px 22px;border-radius:10px;font-weight:600}
                    a:hover{background:#ea580c}
                  </style>
                </head>
                <body>
                  <div class="card">
                    <h1>%s</h1>
                    <p>%s</p>
                    <a href="%s">%s</a>
                  </div>
                </body>
                </html>
                """.formatted(title, accent, title, message, loginUrl, buttonLabel);
        response.getWriter().write(html);
    }

    private static String escapeHtml(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
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

package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.service.MailService;
import com.ecommerce.sellerbackend.service.PlatformIntegrationSettings;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    private final PlatformIntegrationSettings integrationSettings;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name:Flint & Thread Seller}")
    private String fromName;

    @Value("${invoice.seller.email:support@flintnthread.in}")
    private String supportEmail;

    @Value("${invoice.seller.phone:+91 9063499092}")
    private String supportPhone;

    @Value("${app.auth.email-verification-expiry-hours:1}")
    private int emailVerificationExpiryHours;

    @Value("${app.auth.email-verification-otp-minutes:10}")
    private int emailOtpExpiryMinutes;

    @Value("${app.mail.dev-mode:false}")
    private boolean mailDevMode;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Override
    public void sendEmailVerificationLinkEmail(String toEmail, String recipientName, String verifyLink) {
        if (mailDevMode) {
            log.warn("[MAIL DEV] Verification link for {} -> {}", maskEmail(toEmail), verifyLink);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Seller Platform Email Verification - Flint & Thread");
            helper.setText(buildEmailVerificationLinkHtml(recipientName, verifyLink), true);
            mailSender.send(message);
            log.info("Verification link email sent to {}", maskEmail(toEmail));
        } catch (MessagingException ex) {
            log.error("Failed to compose verification link email for {}", maskEmail(toEmail), ex);
            throw new IllegalStateException("Unable to send verification email. Please try again later.");
        } catch (org.springframework.mail.MailException ex) {
            log.error("SendGrid rejected verification link email for {}", maskEmail(toEmail), ex);
            throw new IllegalStateException("Verification email could not be sent. Please try again later.");
        } catch (Exception ex) {
            log.error("Failed to send verification link email for {}", maskEmail(toEmail), ex);
            throw new IllegalStateException("Unable to send verification email. Please try again later.");
        }
    }

    @Override
    public void sendEmailVerificationOtpEmail(String toEmail, String recipientName, String otp) {
        if (mailDevMode) {
            log.warn("[MAIL DEV] Verification OTP for {} -> {}", maskEmail(toEmail), otp);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("OTP Verification - Flint & Thread");
            helper.setText(buildEmailVerificationOtpHtml(recipientName, otp), true);
            mailSender.send(message);
            log.info("Verification OTP email sent to {}", maskEmail(toEmail));
        } catch (MessagingException ex) {
            log.error("Failed to compose verification OTP email for {}", maskEmail(toEmail), ex);
            throw new IllegalStateException("Unable to send verification code. Please try again later.");
        } catch (org.springframework.mail.MailException ex) {
            log.error("SendGrid rejected verification OTP email for {}", maskEmail(toEmail), ex);
            throw new IllegalStateException("Verification code email could not be sent. Please try again later.");
        } catch (Exception ex) {
            log.error("Failed to send verification OTP email for {}", maskEmail(toEmail), ex);
            throw new IllegalStateException("Unable to send verification code. Please try again later.");
        }
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String recipientName, String resetLink) {
        if (mailDevMode) {
            log.warn("[MAIL DEV] Password reset for {} -> {}", maskEmail(toEmail), resetLink);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Reset your seller account password");
            helper.setText(buildPasswordResetHtml(recipientName, resetLink), true);
            mailSender.send(message);
            log.info("Password reset email sent to {}", maskEmail(toEmail));
        } catch (MessagingException ex) {
            log.error("Failed to compose password reset email for {}", maskEmail(toEmail), ex);
            throw new IllegalStateException("Unable to send password reset email. Please try again later.");
        } catch (org.springframework.mail.MailException ex) {
            log.error("SendGrid rejected password reset email for {}", maskEmail(toEmail), ex);
            throw new IllegalStateException(
                    "Unable to send password reset email. Please verify SendGrid sender and API key, then try again.");
        } catch (Exception ex) {
            log.error("Failed to send password reset email for {}", maskEmail(toEmail), ex);
            throw new IllegalStateException("Unable to send password reset email. Please try again later.");
        }
    }

    @Override
    public void sendLoginSecurityAlertEmail(
            String toEmail,
            String recipientName,
            String loginTime,
            String device,
            String location,
            String ipAddress) {
        if (mailDevMode) {
            log.warn("[MAIL DEV] Login security alert for {} (device={}, ip={})", maskEmail(toEmail), device, ipAddress);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Security Alert - New Device Login Detected");
            helper.setText(
                    buildLoginSecurityAlertHtml(recipientName, loginTime, device, location, ipAddress),
                    true
            );
            mailSender.send(message);
            log.info("Login security alert sent to {}", maskEmail(toEmail));
        } catch (MessagingException ex) {
            log.error("Failed to compose login security alert for {}", maskEmail(toEmail), ex);
        } catch (Exception ex) {
            log.error("Failed to send login security alert for {}", maskEmail(toEmail), ex);
        }
    }

    @Override
    public boolean isRegistrationInvoiceEmailConfigured() {
        String apiKey = integrationSettings.getSendGridApiKey();
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public boolean sendRegistrationPaymentSuccessEmail(
            String toEmail,
            String recipientName,
            String invoiceNumber,
            String displayOrderNumber,
            String paymentId,
            int amountInPaise,
            byte[] invoicePdf) {
        if (mailDevMode) {
            log.warn("[MAIL DEV] Registration payment email for {} (invoice={}, paymentId={})",
                    maskEmail(toEmail), invoiceNumber, paymentId);
            return true;
        }
        if (!isRegistrationInvoiceEmailConfigured()) {
            log.error("SendGrid API key is not configured (spring.mail.password / SENDGRID_API_KEY)");
            return false;
        }
        if (invoicePdf == null || invoicePdf.length == 0) {
            log.error("Registration invoice email skipped — PDF is empty for {}", maskEmail(toEmail));
            return false;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setReplyTo(supportEmail);
            helper.setSubject("Registration payment successful - Flint & Thread");
            helper.setText(buildRegistrationPaymentSuccessHtml(
                    recipientName, invoiceNumber, displayOrderNumber, paymentId, amountInPaise), true);
            helper.addAttachment(safeAttachmentName(invoiceNumber), new ByteArrayResource(invoicePdf), "application/pdf");
            mailSender.send(message);
            log.info("Registration payment email sent to {}", maskEmail(toEmail));
            return true;
        } catch (MessagingException ex) {
            log.error("Failed to compose registration payment email for {}", maskEmail(toEmail), ex);
        } catch (org.springframework.mail.MailException ex) {
            log.error("SendGrid rejected registration payment email for {}", maskEmail(toEmail), ex);
        } catch (Exception ex) {
            log.error("Failed to send registration payment email for {}", maskEmail(toEmail), ex);
        }
        return false;
    }

    private String safeAttachmentName(String invoiceNumber) {
        String base = invoiceNumber != null ? invoiceNumber.replaceAll("[^a-zA-Z0-9._-]", "_") : "invoice";
        if (base.isBlank()) {
            base = "invoice";
        }
        return base + ".pdf";
    }

    @Override
    public void sendSubscriptionRenewalReminderEmail(
            String toEmail,
            String recipientName,
            String expiryDate,
            int renewalAmountInr) {
        if (mailDevMode) {
            log.warn("[MAIL DEV] Subscription renewal reminder for {} (expires={})",
                    maskEmail(toEmail), expiryDate);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Seller subscription renewal pending - Flint & Thread");
            helper.setText(buildSubscriptionRenewalReminderHtml(
                    recipientName, expiryDate, renewalAmountInr), true);
            mailSender.send(message);
            log.info("Subscription renewal reminder sent to {}", maskEmail(toEmail));
        } catch (Exception ex) {
            log.error("Failed to send subscription renewal reminder for {}", maskEmail(toEmail), ex);
        }
    }

    private String buildSubscriptionRenewalReminderHtml(
            String recipientName,
            String expiryDate,
            int renewalAmountInr) {
        String name = recipientName != null && !recipientName.isBlank() ? recipientName : "Seller";
        int year = java.time.Year.now().getValue();
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family:Arial,sans-serif;background:#f8fafc;padding:24px;margin:0;">
                  <div style="max-width:560px;margin:0 auto;background:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #e5e7eb;">
                    <div style="background:linear-gradient(135deg,#F97316 0%%,#1E3A6E 100%%);padding:28px 32px;text-align:center;">
                      <h1 style="color:#ffffff;margin:0 0 8px;font-size:22px;">Payment Pending</h1>
                      <p style="color:#fff7ed;margin:0;font-size:15px;">Annual seller subscription renewal</p>
                    </div>
                    <div style="padding:32px;">
                      <p style="color:#111827;font-size:20px;font-weight:bold;line-height:1.4;margin:0 0 16px;">Hello %s,</p>
                      <p style="color:#374151;line-height:1.7;margin:0 0 16px;font-size:15px;">
                        Your annual seller subscription expired on <strong>%s</strong>.
                        Please renew your subscription of Rs %d per annum to continue accessing your seller dashboard.
                      </p>
                      <p style="color:#374151;line-height:1.6;margin:0 0 12px;font-size:14px;">
                        Log in to your seller account and complete the renewal payment from Settings or the renewal screen.
                      </p>
                      <p style="color:#9ca3af;font-size:12px;margin-top:24px;margin-bottom:6px;text-align:center;">
                        &copy; %d Flint &amp; Thread. All rights reserved.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """
                .formatted(name, expiryDate, renewalAmountInr, year);
    }

    private static final String EMAIL_VERIFICATION_HEADER = """
            <div style="background:linear-gradient(135deg,#F97316 0%%,#B45309 100%%);padding:28px 32px;text-align:center;">
              <h1 style="color:#ffffff;margin:0 0 8px;font-size:22px;font-family:Arial,sans-serif;">Welcome to Flint &amp; Thread!</h1>
              <p style="color:#fff7ed;margin:0;font-size:15px;font-family:Arial,sans-serif;">Seller Platform Email Verification</p>
            </div>
            """;

    private static final String OTP_VERIFICATION_HEADER = """
            <div style="background:linear-gradient(135deg,#F97316 0%%,#B45309 100%%);padding:28px 32px;text-align:center;">
              <h1 style="color:#ffffff;margin:0 0 8px;font-size:22px;font-family:Arial,sans-serif;">OTP Verification</h1>
              <p style="color:#fff7ed;margin:0;font-size:15px;font-family:Arial,sans-serif;">Flint &amp; Thread Seller Platform</p>
            </div>
            """;

    private String buildEmailVerificationLinkHtml(String recipientName, String verifyLink) {
        String name = recipientName != null && !recipientName.isBlank() ? recipientName : "Seller";
        int year = java.time.Year.now().getValue();
        String expiryLabel = emailVerificationExpiryHours == 1
                ? "1 hour"
                : emailVerificationExpiryHours + " hours";
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family:Arial,sans-serif;background:#f3f4f6;padding:24px;margin:0;">
                  <div style="max-width:560px;margin:0 auto;background:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #e5e7eb;">
                    %s
                    <div style="padding:32px;">
                      <p style="color:#111827;font-size:20px;font-weight:bold;line-height:1.4;margin:0 0 16px;">Hello %s,</p>
                      <p style="color:#374151;line-height:1.7;margin:0 0 20px;font-size:15px;">
                        Thank you for registering as a seller on our platform! To complete your registration and start
                        selling, please verify your email address.
                      </p>
                      <p style="color:#374151;line-height:1.6;margin:0 0 16px;font-size:15px;">
                        Click the button below to verify your email:
                      </p>
                      <p style="text-align:center;margin:28px 0;">
                        <a href="%s" style="background:linear-gradient(135deg,#F97316 0%%,#B45309 100%%);color:#ffffff;text-decoration:none;padding:14px 32px;border-radius:8px;font-weight:bold;display:inline-block;font-size:16px;">
                          Verify Email Address
                        </a>
                      </p>
                      <p style="color:#374151;line-height:1.6;margin:24px 0 12px;font-size:14px;">
                        If the button doesn't work, copy and paste this link into your browser:
                      </p>
                      <div style="background:#eff6ff;border:1px solid #dbeafe;border-radius:8px;padding:12px;margin-bottom:20px;">
                        <a href="%s" style="color:#2563eb;font-size:13px;word-break:break-all;text-decoration:none;">%s</a>
                      </div>
                      <p style="color:#374151;line-height:1.6;margin:0 0 20px;font-size:14px;">
                        <strong>Important:</strong> This verification link will expire in %s for security reasons.
                      </p>
                      <div style="background:#eff6ff;border-left:4px solid #1E3A6E;border-radius:8px;padding:16px;margin-top:8px;">
                        <p style="margin:0 0 8px;font-weight:bold;color:#1E3A6E;font-size:15px;">Need Help?</p>
                        <p style="margin:0 0 8px;color:#374151;font-size:14px;line-height:1.6;">
                          If you have any questions or need assistance, please contact our support team:
                        </p>
                        <p style="margin:0 0 6px;color:#374151;font-size:14px;"><strong>Email:</strong>
                          <a href="mailto:%s" style="color:#2563eb;text-decoration:none;">%s</a>
                        </p>
                        <p style="margin:0;color:#374151;font-size:14px;"><strong>Phone:</strong> %s</p>
                      </div>
                      <p style="color:#9ca3af;font-size:12px;margin-top:24px;margin-bottom:6px;text-align:center;">
                        &copy; %d Flint &amp; Thread. All rights reserved.
                      </p>
                      <p style="color:#9ca3af;font-size:11px;margin:0;text-align:center;">
                        This is an automated email. Please do not reply to this email.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """
                .formatted(
                        EMAIL_VERIFICATION_HEADER,
                        name,
                        verifyLink,
                        verifyLink,
                        verifyLink,
                        expiryLabel,
                        supportEmail,
                        supportEmail,
                        supportPhone,
                        year
                );
    }

    private String buildLoginSecurityAlertHtml(
            String recipientName,
            String loginTime,
            String device,
            String location,
            String ipAddress) {
        String name = recipientName != null && !recipientName.isBlank() ? recipientName : "Seller";
        String safeTime = loginTime != null && !loginTime.isBlank() ? loginTime : "Unknown time";
        String safeDevice = device != null && !device.isBlank() ? device : "Unknown Device";
        String safeLocation = location != null && !location.isBlank() ? location : "Unknown Location";
        String safeIp = ipAddress != null && !ipAddress.isBlank() ? ipAddress : "Unknown";
        int year = java.time.Year.now().getValue();
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family:Arial,sans-serif;background:#f3f4f6;padding:24px;margin:0;">
                  <div style="max-width:560px;margin:0 auto;background:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #e5e7eb;">
                    <div style="background:linear-gradient(135deg,#F97316 0%%,#92400E 100%%);padding:28px 32px;text-align:center;">
                      <p style="color:#ffffff;margin:0 0 8px;font-size:22px;font-weight:bold;font-family:Arial,sans-serif;">&#128274; Security Alert</p>
                      <p style="color:#fff7ed;margin:0;font-size:15px;font-family:Arial,sans-serif;">New Device Login Detected</p>
                    </div>
                    <div style="padding:32px;">
                      <p style="color:#111827;font-size:20px;font-weight:bold;line-height:1.4;margin:0 0 16px;">Hello %s,</p>
                      <p style="color:#374151;line-height:1.7;margin:0 0 20px;font-size:15px;">
                        We detected a login to your Flint &amp; Thread seller account from a new device or location.
                      </p>
                      <div style="border-left:4px solid #F97316;padding:16px 16px 16px 20px;margin:0 0 20px;background:#ffffff;">
                        <p style="margin:0 0 12px;font-weight:bold;color:#B45309;font-size:15px;">Login Details:</p>
                        <p style="margin:0 0 8px;color:#374151;font-size:14px;line-height:1.6;"><strong>Time:</strong> %s</p>
                        <p style="margin:0 0 8px;color:#374151;font-size:14px;line-height:1.6;"><strong>Device:</strong> %s</p>
                        <p style="margin:0 0 8px;color:#374151;font-size:14px;line-height:1.6;"><strong>Location:</strong> %s</p>
                        <p style="margin:0;color:#374151;font-size:14px;line-height:1.6;"><strong>IP Address:</strong> %s</p>
                      </div>
                      <div style="background:#fef9c3;border:1px solid #fde68a;border-radius:8px;padding:16px;margin-bottom:20px;">
                        <p style="margin:0 0 10px;font-weight:bold;color:#111827;font-size:15px;">&#9888;&#65039; Was this you?</p>
                        <p style="margin:0 0 10px;color:#374151;font-size:14px;line-height:1.6;">
                          If you recognize this login, you can ignore this email. If you don't recognize this activity, please:
                        </p>
                        <ul style="margin:0;padding-left:20px;color:#374151;font-size:14px;line-height:1.8;">
                          <li>Change your password immediately</li>
                          <li>Review your account for any unauthorized changes</li>
                          <li>Contact our support team</li>
                        </ul>
                      </div>
                      <p style="color:#374151;font-size:14px;line-height:1.6;margin:0 0 8px;">For security assistance, contact us:</p>
                      <p style="margin:0 0 6px;color:#374151;font-size:14px;"><strong>Email:</strong>
                        <a href="mailto:%s" style="color:#2563eb;text-decoration:none;">%s</a>
                      </p>
                      <p style="margin:0 0 20px;color:#374151;font-size:14px;"><strong>Phone:</strong> %s</p>
                      <p style="color:#9ca3af;font-size:12px;margin-top:24px;margin-bottom:6px;text-align:center;">
                        &copy; %d Flint &amp; Thread. All rights reserved.
                      </p>
                      <p style="color:#9ca3af;font-size:11px;margin:0;text-align:center;">
                        This is an automated security alert.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """
                .formatted(
                        name,
                        safeTime,
                        safeDevice,
                        safeLocation,
                        safeIp,
                        supportEmail,
                        supportEmail,
                        supportPhone,
                        year
                );
    }

    private String buildEmailVerificationOtpHtml(String recipientName, String otp) {
        String name = recipientName != null && !recipientName.isBlank() ? recipientName : "Seller";
        int year = java.time.Year.now().getValue();
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family:Arial,sans-serif;background:#f3f4f6;padding:24px;margin:0;">
                  <div style="max-width:560px;margin:0 auto;background:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #e5e7eb;">
                    %s
                    <div style="padding:32px;">
                      <p style="color:#111827;font-size:20px;font-weight:bold;line-height:1.4;margin:0 0 16px;">Hello %s,</p>
                      <p style="color:#374151;line-height:1.7;margin:0 0 20px;font-size:15px;">
                        Your One-Time Password (OTP) for email verification is:
                      </p>
                      <div style="border:2px solid #F97316;border-radius:10px;padding:24px 16px;margin:0 0 12px;text-align:center;background:#ffffff;">
                        <p style="margin:0;font-size:36px;font-weight:bold;color:#F97316;letter-spacing:8px;font-family:Arial,sans-serif;">%s</p>
                      </div>
                      <p style="color:#6b7280;font-size:14px;text-align:center;margin:0 0 24px;">
                        Enter this code to verify your email
                      </p>
                      <div style="background:#fffbeb;border:1px solid #fde68a;border-radius:8px;padding:16px;margin:0 0 24px;">
                        <p style="margin:0 0 10px;font-weight:bold;color:#111827;font-size:15px;">&#9888;&#65039; Security Notice:</p>
                        <ul style="margin:0;padding-left:20px;color:#374151;font-size:14px;line-height:1.8;">
                          <li>This OTP is valid for %d minutes only</li>
                          <li>Do not share this code with anyone</li>
                          <li>If you didn't request this, please contact support immediately</li>
                        </ul>
                      </div>
                      <p style="color:#374151;font-size:14px;line-height:1.6;margin:0 0 8px;text-align:center;">
                        Need help? Contact our support team:
                      </p>
                      <p style="margin:0 0 6px;color:#374151;font-size:14px;text-align:center;">
                        <strong>Email:</strong>
                        <a href="mailto:%s" style="color:#2563eb;text-decoration:none;">%s</a>
                        &nbsp;|&nbsp;
                        <strong>Phone:</strong> %s
                      </p>
                      <p style="color:#9ca3af;font-size:12px;margin-top:24px;margin-bottom:0;text-align:center;">
                        &copy; %d Flint &amp; Thread. All rights reserved.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """
                .formatted(
                        OTP_VERIFICATION_HEADER,
                        name,
                        otp,
                        emailOtpExpiryMinutes,
                        supportEmail,
                        supportEmail,
                        supportPhone,
                        year
                );
    }

    private String buildPasswordResetHtml(String recipientName, String resetLink) {
        String name = recipientName != null && !recipientName.isBlank() ? recipientName : "Seller";
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family:Arial,sans-serif;background:#f8fafc;padding:24px;margin:0;">
                  <div style="max-width:560px;margin:0 auto;background:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #e5e7eb;">
                    <div style="background:linear-gradient(135deg,#F97316 0%%,#1E3A6E 100%%);padding:28px 32px;text-align:center;">
                      <h1 style="color:#ffffff;margin:0 0 8px;font-size:22px;">Password Reset</h1>
                      <p style="color:#fff7ed;margin:0;font-size:15px;">Flint &amp; Thread Seller Account</p>
                    </div>
                    <div style="padding:32px;">
                      <p style="color:#374151;line-height:1.6;margin:0 0 12px;">Hello %s,</p>
                      <p style="color:#374151;line-height:1.6;margin:0 0 16px;">
                        We received a request to reset your seller account password. Click the button below to choose a new password.
                        This link expires in 1 hour.
                      </p>
                      <p style="text-align:center;margin:28px 0;">
                        <a href="%s" style="background:linear-gradient(135deg,#F97316 0%%,#B45309 100%%);color:#ffffff;text-decoration:none;padding:14px 32px;border-radius:999px;font-weight:bold;display:inline-block;">
                          Reset Password
                        </a>
                      </p>
                      <p style="color:#6b7280;font-size:13px;line-height:1.5;">
                        If the button does not work, copy and paste this link into your browser:<br/>
                        <a href="%s" style="color:#376197;word-break:break-all;">%s</a>
                      </p>
                      <p style="color:#9ca3af;font-size:12px;margin-top:24px;margin-bottom:0;">
                        If you did not request a password reset, you can safely ignore this email.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """
                .formatted(name, resetLink, resetLink, resetLink);
    }

    private String buildRegistrationPaymentSuccessHtml(
            String recipientName,
            String invoiceNumber,
            String displayOrderNumber,
            String paymentId,
            int amountInPaise) {
        String name = recipientName != null && !recipientName.isBlank() ? recipientName : "Seller";
        int year = java.time.Year.now().getValue();
        String amount = String.format(java.util.Locale.ENGLISH, "%.2f", amountInPaise / 100.0);
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family:Arial,sans-serif;background:#f8fafc;padding:24px;margin:0;">
                  <div style="max-width:560px;margin:0 auto;background:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #e5e7eb;">
                    <div style="background:linear-gradient(135deg,#F97316 0%%,#1E3A6E 100%%);padding:28px 32px;text-align:center;">
                      <h1 style="color:#ffffff;margin:0 0 8px;font-size:22px;">Payment Successful</h1>
                      <p style="color:#fff7ed;margin:0;font-size:15px;">Annual seller subscription fee received</p>
                    </div>
                    <div style="padding:32px;">
                      <p style="color:#111827;font-size:20px;font-weight:bold;line-height:1.4;margin:0 0 16px;">Hello %s,</p>
                      <p style="color:#374151;line-height:1.7;margin:0 0 16px;font-size:15px;">
                        Your annual seller subscription payment of Rs %s (incl. GST) has been received successfully.
                      </p>
                      <div style="background:#f8fafc;border:1px solid #e5e7eb;border-radius:10px;padding:16px;margin:16px 0;">
                        <p style="margin:0 0 8px;color:#374151;font-size:14px;"><strong>Invoice:</strong> %s</p>
                        <p style="margin:0 0 8px;color:#374151;font-size:14px;"><strong>Order:</strong> #%s</p>
                        <p style="margin:0 0 8px;color:#374151;font-size:14px;"><strong>Payment ID:</strong> %s</p>
                        <p style="margin:0;color:#374151;font-size:14px;"><strong>Amount:</strong> Rs %s</p>
                      </div>
                      <p style="color:#374151;line-height:1.6;margin:0 0 12px;font-size:14px;">
                        We have attached your invoice PDF with this email.
                      </p>
                      <p style="color:#9ca3af;font-size:12px;margin-top:24px;margin-bottom:6px;text-align:center;">
                        &copy; %d Flint &amp; Thread. All rights reserved.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """
                .formatted(name, amount, invoiceNumber, displayOrderNumber, paymentId, amount, year);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String maskedLocal = local.length() <= 2 ? "**" : local.charAt(0) + "***";
        return maskedLocal + "@" + parts[1];
    }
}


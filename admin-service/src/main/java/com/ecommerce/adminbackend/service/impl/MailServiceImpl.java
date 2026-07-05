package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.service.MailService;
import com.ecommerce.adminbackend.util.OrderStatusUpdateEmailBuilder;
import com.ecommerce.adminbackend.util.SellerAccountStatusEmailBuilder;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name:Flint & Thread}")
    private String fromName;

    @Value("${app.invoice.company-email:support@flintnthread.in}")
    private String supportEmail;

    @Value("${app.invoice.company-phone:+91 9063499092}")
    private String supportPhone;

    @Value("${app.mail.dev-mode:false}")
    private boolean mailDevMode;

    @Value("${app.auth.email-verification-expiry-hours:24}")
    private int emailVerificationExpiryHours;

    @Override
    public void sendEmailVerificationLinkEmail(String toEmail, String recipientName, String verifyLink) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("[MAIL] Skipping verification email — recipient missing");
            return;
        }

        if (mailDevMode) {
            log.warn("[MAIL DEV] Verification link for {} -> {}", maskEmail(toEmail), verifyLink);
            return;
        }

        String subject = "Seller Platform Email Verification - Flint & Thread";
        String html = buildEmailVerificationLinkHtml(recipientName, verifyLink);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail.trim());
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Verification link email sent to {}", maskEmail(toEmail));
        } catch (MessagingException ex) {
            log.error("Failed to compose verification link email for {}", maskEmail(toEmail), ex);
            throw new IllegalStateException("Unable to send verification email. Please try again later.");
        } catch (Exception ex) {
            log.error("Failed to send verification link email for {}", maskEmail(toEmail), ex);
            throw new IllegalStateException("Unable to send verification email. Please try again later.");
        }
    }

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
                    <div style="background:linear-gradient(135deg,#1E3A6E 0%%,#2563EB 100%%);padding:28px 32px;text-align:center;">
                      <p style="color:#ffffff;margin:0 0 8px;font-size:22px;font-weight:bold;font-family:Arial,sans-serif;">Email Verification</p>
                      <p style="color:#dbeafe;margin:0;font-size:15px;font-family:Arial,sans-serif;">Flint &amp; Thread Seller Platform</p>
                    </div>
                    <div style="padding:32px;">
                      <p style="color:#111827;font-size:20px;font-weight:bold;line-height:1.4;margin:0 0 16px;">Hello %s,</p>
                      <p style="color:#374151;line-height:1.7;margin:0 0 20px;font-size:15px;">
                        Please verify your email address to complete your seller registration.
                      </p>
                      <p style="text-align:center;margin:28px 0;">
                        <a href="%s" style="background:linear-gradient(135deg,#1E3A6E 0%%,#2563EB 100%%);color:#ffffff;text-decoration:none;padding:14px 32px;border-radius:8px;font-weight:bold;display:inline-block;font-size:16px;">
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
                      <p style="color:#9ca3af;font-size:12px;margin-top:24px;margin-bottom:6px;text-align:center;">
                        &copy; %d Flint &amp; Thread. All rights reserved.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """
                .formatted(name, verifyLink, verifyLink, verifyLink, expiryLabel, year);
    }

    @Override
    public void sendOrderStatusUpdateEmail(
            String toEmail,
            String customerName,
            String orderNumber,
            String statusLabel,
            String comment) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("[MAIL] Skipping order status email — recipient missing for {}", orderNumber);
            return;
        }

        String subject = OrderStatusUpdateEmailBuilder.buildSubject(orderNumber);
        String html = OrderStatusUpdateEmailBuilder.buildHtml(
                customerName,
                orderNumber,
                statusLabel,
                comment,
                supportEmail,
                supportPhone);

        if (mailDevMode) {
            log.warn("[MAIL DEV] Order status update for {} -> {} ({})", maskEmail(toEmail), orderNumber, statusLabel);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail.trim());
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Order status update email sent to {} for {}", maskEmail(toEmail), orderNumber);
        } catch (MessagingException ex) {
            log.error("Failed to compose order status email for {}", maskEmail(toEmail), ex);
            throw new IllegalStateException("Unable to send customer notification email.");
        } catch (Exception ex) {
            log.error("Failed to send order status email for {}", maskEmail(toEmail), ex);
            throw new IllegalStateException("Unable to send customer notification email.");
        }
    }

    @Override
    public void sendSellerAccountStatusEmail(
            String toEmail,
            String sellerName,
            String statusLabel,
            String reason) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("[MAIL] Skipping seller account status email — recipient missing");
            return;
        }

        String subject = SellerAccountStatusEmailBuilder.buildSubject(statusLabel);
        String html = SellerAccountStatusEmailBuilder.buildHtml(
                sellerName,
                statusLabel,
                reason,
                supportEmail,
                supportPhone);

        if (mailDevMode) {
            log.warn("[MAIL DEV] Seller account status for {} -> {} ({})",
                    maskEmail(toEmail), statusLabel, reason);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail.trim());
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Seller account status email sent to {} ({})", maskEmail(toEmail), statusLabel);
        } catch (MessagingException ex) {
            log.error("Failed to compose seller account status email for {}", maskEmail(toEmail), ex);
            throw new IllegalStateException("Unable to send seller notification email.");
        } catch (Exception ex) {
            log.error("Failed to send seller account status email for {}", maskEmail(toEmail), ex);
            throw new IllegalStateException("Unable to send seller notification email.");
        }
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

package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.entity.EmailLogStatus;
import com.ecommerce.authdemo.exception.EmailSendException;

import com.ecommerce.authdemo.mail.OrderConfirmationEmailBuilder;
import com.ecommerce.authdemo.mail.OrderConfirmationEmailModel;

import com.ecommerce.authdemo.util.EmailHtmlTemplates;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    @Value("${app.mail.from:support@flintnthread.in}")
    private String fromEmail;

    @Value("${app.mail.from-name:Flint & Thread}")
    private String fromName;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private EmailLogService emailLogService;

    public void sendOtpEmail(String toEmail, String otp) {
        String digits = otp.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            digits = otp;
        }
        sendHtmlEmail(
                toEmail,
                "FlintnThread Login OTP — " + digits,
                "otp",
                EmailHtmlTemplates.buildOtpEmailHtml(otp),
                EmailHtmlTemplates.buildOtpPlainText(otp)
        );
    }

    public void sendWelcomeEmail(String toEmail, String displayName, String username, String referralCode) {
        sendHtmlEmail(
                toEmail,
                "🎉 Welcome to Flint & Thread - Your Journey Begins Now!",
                "welcome",
                EmailHtmlTemplates.buildWelcomeEmailHtml(displayName, username, referralCode),
                EmailHtmlTemplates.buildWelcomePlainText(displayName, username, referralCode)
        );
    }

    private void sendHtmlEmail(String toEmail, String subject, String emailType, String htmlBody) {
        sendHtmlEmail(toEmail, subject, emailType, htmlBody, null);
    }

    private void sendHtmlEmail(
            String toEmail,
            String subject,
            String emailType,
            String htmlBody,
            String plainTextBody
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            boolean multipart = plainTextBody != null && !plainTextBody.isBlank();
            MimeMessageHelper helper = new MimeMessageHelper(message, multipart, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            if (multipart) {
                helper.setText(plainTextBody, htmlBody);
            } else {
                helper.setText(htmlBody, true);
            }

            mailSender.send(message);
            logEmailSafe(null, emailType, toEmail, subject, EmailLogStatus.sent, null);
            log.info("HTML {} email sent to: {} ({} chars)", emailType, toEmail, htmlBody.length());
        } catch (MessagingException | MailException | java.io.UnsupportedEncodingException e) {
            logEmailSafe(null, emailType, toEmail, subject, EmailLogStatus.failed, e.getMessage());
            log.error("Failed to send {} email to: {}", emailType, toEmail, e);
            throw new EmailSendException("Unable to send OTP to email");
        }
    }

    private void logEmailSafe(
            Integer userId,
            String emailType,
            String toEmail,
            String subject,
            EmailLogStatus status,
            String error
    ) {
        try {
            emailLogService.createLog(userId, emailType, toEmail, subject, status, error);
        } catch (Exception logError) {
            log.warn("[EMAIL] Could not write email log for {}: {}", toEmail, logError.getMessage());
        }
    }

    /**
     * Branded HTML order confirmation — failures are logged but do not throw
     * (order placement must not roll back if email fails).
     */
    public void sendOrderConfirmationEmail(Integer userId, OrderConfirmationEmailModel model) {
        if (model == null || model.customerEmail() == null || model.customerEmail().isBlank()) {
            log.warn("[EMAIL] skip order confirmation — no recipient");
            return;
        }

        String recipientType = model.recipientType();
        String subject = OrderConfirmationEmailBuilder.buildSubject(model.orderNumber(), recipientType);
        String html = OrderConfirmationEmailBuilder.buildHtml(model);
        String recipient = model.customerEmail().trim();
        String emailType = resolveOrderEmailType(recipientType);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mimeMessage);

            emailLogService.createLog(
                    userId,
                    emailType,
                    recipient,
                    subject,
                    EmailLogStatus.sent,
                    null
            );
            log.info("[EMAIL] {} sent to {} for order {}", emailType, recipient, model.orderNumber());
        } catch (MailException | MessagingException e) {
            emailLogService.createLog(
                    userId,
                    emailType,
                    recipient,
                    subject,
                    EmailLogStatus.failed,
                    e.getMessage()
            );
            log.error("[EMAIL] Failed {} to {}: {}", emailType, recipient, e.getMessage(), e);
        } catch (Exception e) {
            emailLogService.createLog(
                    userId,
                    emailType,
                    recipient,
                    subject,
                    EmailLogStatus.failed,
                    e.getMessage()
            );
            log.error("[EMAIL] Unexpected {} failure: {}", emailType, e.getMessage(), e);
        }
    }

    private String resolveOrderEmailType(String recipientType) {
        if (OrderConfirmationEmailModel.RECIPIENT_SELLER.equals(recipientType)) {
            return "order_notification_seller";
        }
        if (OrderConfirmationEmailModel.RECIPIENT_ADMIN.equals(recipientType)) {
            return "order_notification_admin";
        }
        return "order_confirmation";
    }
}


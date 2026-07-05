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

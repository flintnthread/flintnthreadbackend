package com.ecommerce.adminbackend.service;

public interface MailService {

    void sendOrderStatusUpdateEmail(
            String toEmail,
            String customerName,
            String orderNumber,
            String statusLabel,
            String comment);

    void sendSellerAccountStatusEmail(
            String toEmail,
            String sellerName,
            String statusLabel,
            String reason);

    void sendEmailVerificationLinkEmail(String toEmail, String recipientName, String verifyLink);
}

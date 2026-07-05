package com.ecommerce.sellerbackend.service;

public interface MailService {

    void sendPasswordResetEmail(String toEmail, String recipientName, String resetLink);

    /** Step 1 after signup: link only — user must click to receive OTP. */
    void sendEmailVerificationLinkEmail(String toEmail, String recipientName, String verifyLink);

    /** Step 2 after link click: 6-digit OTP only. */
    void sendEmailVerificationOtpEmail(String toEmail, String recipientName, String otp);

    void sendLoginSecurityAlertEmail(
            String toEmail,
            String recipientName,
            String loginTime,
            String device,
            String location,
            String ipAddress
    );

    boolean sendRegistrationPaymentSuccessEmail(
            String toEmail,
            String recipientName,
            String invoiceNumber,
            String displayOrderNumber,
            String paymentId,
            int amountInPaise,
            byte[] invoicePdf
    );

    boolean isRegistrationInvoiceEmailConfigured();

    void sendSubscriptionRenewalReminderEmail(
            String toEmail,
            String recipientName,
            String expiryDate,
            int renewalAmountInr
    );
}

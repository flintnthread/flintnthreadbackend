package com.ecommerce.adminbackend.service;

public interface MailService {

    void sendOrderStatusUpdateEmail(
            String toEmail,
            String customerName,
            String orderNumber,
            String statusLabel,
            String comment);
}

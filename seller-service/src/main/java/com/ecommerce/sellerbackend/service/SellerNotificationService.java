package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.SellerNotificationResponse;

import java.util.List;

public interface SellerNotificationService {
    List<SellerNotificationResponse> listForSeller(Long sellerId);

    void markRead(Long sellerId, Long notificationId);

    void markAllRead(Long sellerId);
}

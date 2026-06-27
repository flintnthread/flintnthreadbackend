package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.dto.SellerNotificationResponse;
import com.ecommerce.sellerbackend.entity.SellerNotification;
import com.ecommerce.sellerbackend.exception.ResourceNotFoundException;
import com.ecommerce.sellerbackend.repository.SellerNotificationRepository;
import com.ecommerce.sellerbackend.service.SellerNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SellerNotificationServiceImpl implements SellerNotificationService {

    private final SellerNotificationRepository sellerNotificationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SellerNotificationResponse> listForSeller(Long sellerId) {
        return sellerNotificationRepository.findBySellerIdOrderByCreatedAtDesc(sellerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void markRead(Long sellerId, Long notificationId) {
        SellerNotification notification = sellerNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found."));
        if (!sellerId.equals(notification.getSellerId())) {
            throw new ResourceNotFoundException("Notification not found.");
        }
        notification.setRead(true);
        sellerNotificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllRead(Long sellerId) {
        List<SellerNotification> items = sellerNotificationRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
        for (SellerNotification item : items) {
            item.setRead(true);
        }
        sellerNotificationRepository.saveAll(items);
    }

    private SellerNotificationResponse toResponse(SellerNotification n) {
        String type = inferType(n.getTitle());
        return SellerNotificationResponse.builder()
                .id(n.getId())
                .type(type)
                .title(n.getTitle())
                .body(n.getMessage())
                .time(n.getCreatedAt() != null ? n.getCreatedAt().toString() : "")
                .read(Boolean.TRUE.equals(n.getRead()))
                .build();
    }

    private String inferType(String title) {
        if (title == null) {
            return "payment";
        }
        String lower = title.toLowerCase();
        if (lower.contains("order")) {
            return lower.contains("cancel") ? "order_cancelled" : "new_order";
        }
        if (lower.contains("stock")) {
            return "low_stock";
        }
        if (lower.contains("ticket") || lower.contains("support")) {
            return "tickets";
        }
        return "payment";
    }
}

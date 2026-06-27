package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.NotificationInboxDTO;
import com.ecommerce.authdemo.dto.PushNotificationDetailDTO;
import com.ecommerce.authdemo.dto.PushNotificationRequest;
import com.ecommerce.authdemo.dto.PushNotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PushNotificationService {
    PushNotificationResponse create(PushNotificationRequest request);

    List<PushNotificationResponse> getNotifications(Long userId, String type, Boolean isRead);

    Page<PushNotificationResponse> getNotificationsPaged(Long userId, String type, Boolean isRead, Pageable pageable);

    NotificationInboxDTO getInbox(Long userId, String category, int page, int size);

    PushNotificationDetailDTO getDetail(Integer id, Long userId);

    PushNotificationResponse markAsRead(Integer id);

    PushNotificationResponse markAsUnread(Integer id);

    int markAllAsRead(Long userId);

    long getUnreadCount(Long userId);

    void delete(Integer id);

    void notifyUser(Long userId, String title, String message, String type, String link);

    void sendNotification(String expoPushToken, String title, String message);
}


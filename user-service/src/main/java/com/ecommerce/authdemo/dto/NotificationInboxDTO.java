package com.ecommerce.authdemo.dto;

import lombok.Data;
import org.springframework.data.domain.Page;

@Data
public class NotificationInboxDTO {
    private NotificationInboxSummaryDTO summary;
    private Page<PushNotificationResponse> notifications;
}

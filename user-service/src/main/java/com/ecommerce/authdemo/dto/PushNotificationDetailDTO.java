package com.ecommerce.authdemo.dto;

import lombok.Data;

@Data
public class PushNotificationDetailDTO {
    private PushNotificationResponse notification;
    private NotificationOrderDetailDTO orderDetail;
}

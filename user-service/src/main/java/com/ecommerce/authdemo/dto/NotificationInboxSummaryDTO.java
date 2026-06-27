package com.ecommerce.authdemo.dto;

import lombok.Data;

@Data
public class NotificationInboxSummaryDTO {
    private long total;
    private long unread;
    private long orders;
    private long promotions;
    private long system;
}

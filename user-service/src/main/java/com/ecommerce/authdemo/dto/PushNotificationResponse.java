package com.ecommerce.authdemo.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushNotificationResponse {
    private Integer id;
    private Long userId;
    private String title;
    private String message;
    private String type;
    private String link;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    // Backward compatibility for any stale call sites compiled with Integer userId.
    public static class PushNotificationResponseBuilder {
        public PushNotificationResponseBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public PushNotificationResponseBuilder userId(Integer userId) {
            this.userId = userId == null ? null : userId.longValue();
            return this;
        }
    }
}

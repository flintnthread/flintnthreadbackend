package com.ecommerce.sellerbackend.dto.support;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long id;
    private Long sellerId;
    private Long ticketId;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
}

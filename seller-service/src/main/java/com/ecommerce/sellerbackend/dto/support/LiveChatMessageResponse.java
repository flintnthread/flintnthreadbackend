package com.ecommerce.sellerbackend.dto.support;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveChatMessageResponse {
    private Long id;
    private Integer sellerId;
    private String senderType;
    private String message;
    private LocalDateTime createdAt;
}

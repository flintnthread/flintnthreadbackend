package com.ecommerce.sellerbackend.dto.support;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {

    private Long id;
    private Long ticketId;
    private String senderType;
    private Long senderId;
    private String message;
    private String attachment;
    private LocalDateTime createdAt;
}

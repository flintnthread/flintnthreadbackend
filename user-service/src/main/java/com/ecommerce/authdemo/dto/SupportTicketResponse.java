package com.ecommerce.authdemo.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketResponse {
    private Integer id;
    private Integer customerId;
    private String subject;
    private String type;
    private String message;
    private Integer orderId;
    private String attachmentPath;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

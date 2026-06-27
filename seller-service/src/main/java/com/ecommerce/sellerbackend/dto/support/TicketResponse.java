package com.ecommerce.sellerbackend.dto.support;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponse {

    private Long id;
    private String ticketNumber;
    private Long sellerId;
    private String subject;
    private String category;
    private String priority;
    private String status;
    private Long assignedTo;
    private String lastResponseBy;
    private LocalDateTime lastResponseAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MessageResponse> messages;
}

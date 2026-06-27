package com.ecommerce.authdemo.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketUserReplyResponse {
    private Integer id;
    private Integer ticketId;
    private Integer userId;
    private String message;
    private LocalDateTime createdAt;
}

package com.ecommerce.authdemo.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponseReadResponse {
    private Integer id;
    private Integer userId;
    private Integer responseId;
    private LocalDateTime readAt;
}

package com.ecommerce.authdemo.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponseResponse {
    private Integer id;
    private Integer ticketId;
    private Integer adminId;
    private String response;
    private LocalDateTime createdAt;
}

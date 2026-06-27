package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TicketUserReplyRequest {
    private Integer userId;

    @NotBlank(message = "Message is required")
    private String message;
}

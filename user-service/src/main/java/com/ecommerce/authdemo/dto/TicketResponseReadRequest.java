package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TicketResponseReadRequest {
    @NotNull(message = "User ID is required")
    private Integer userId;

    @NotNull(message = "Response ID is required")
    private Integer responseId;
}

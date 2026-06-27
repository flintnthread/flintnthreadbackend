package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TicketResponseRequest {
    @NotNull(message = "Admin ID is required")
    private Integer adminId;

    @NotBlank(message = "Response is required")
    private String response;
}

package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InvoiceRequest {
    @NotNull(message = "Order ID is required")
    private Integer orderId;

    @Size(max = 255, message = "Invoice path must not exceed 255 characters")
    private String invoicePath;
}

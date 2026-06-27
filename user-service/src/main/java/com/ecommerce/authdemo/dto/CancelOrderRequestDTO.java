package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelOrderRequestDTO {

    @NotBlank(message = "Cancellation reason is required")
    @Size(max = 500, message = "Cancellation reason is too long")
    private String reason;

    private Boolean refundToWallet = true;
}

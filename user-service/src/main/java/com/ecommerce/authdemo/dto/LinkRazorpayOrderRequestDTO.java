package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LinkRazorpayOrderRequestDTO {

    @NotBlank
    private String razorpayOrderId;
}

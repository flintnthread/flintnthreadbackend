package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderRequestDTO {

    @NotNull(message = "Address ID is required")
    @Positive(message = "Address ID must be positive")
    private Long addressId;

    @NotBlank(message = "Payment method is required")
    @Pattern(regexp = "^(credit_card|debit_card|paypal|cash_on_delivery|upi)$", 
             message = "Invalid payment method")
    private String paymentMethod;

    @Size(max = 500, message = "Order notes must not exceed 500 characters")
    private String orderNotes;

    @Pattern(regexp = "^[A-Z0-9]{5,20}$", message = "Invalid coupon code format")
    private String couponCode;

    private Boolean useWallet = false;

    @Min(value = 0, message = "Wallet amount cannot be negative")
    private Double walletAmount = 0.0;

    @Size(max = 50, message = "Razorpay order ID must not exceed 50 characters")
    private String razorpayOrderId;

    /** Optional cart line ids for Buy Now / partial checkout. */
    private java.util.List<Long> itemIds;
}
package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class VerifyRetryPaymentRequestDTO {

    @NotNull
    @Positive
    private Long orderId;

    private String razorpayOrderId;

    private String paymentId;

    private String signature;

    /** Legacy aliases from older clients */
    private String razorpayPaymentId;

    private String razorpaySignature;

    public String resolvedRazorpayOrderId() {
        return firstNonBlank(razorpayOrderId);
    }

    public String resolvedPaymentId() {
        return firstNonBlank(paymentId, razorpayPaymentId);
    }

    public String resolvedSignature() {
        return firstNonBlank(signature, razorpaySignature);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}

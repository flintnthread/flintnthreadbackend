package com.ecommerce.sellerbackend.dto.order;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SellerPaymentDto {
    private String method;
    private String status;
    private String sellerPaymentStatus;
    private boolean paymentCompleted;
    private String transactionId;
    private String paidOn;
    private String bankOrUpiId;
    private String refNo;
    private String razorpayOrderId;
}

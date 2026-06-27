package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WalletTransactionResponse {
    private Long id;
    private String title;
    private String amount;
    private String date;
    private String status;
    private String type;
    private Long orderId;
}

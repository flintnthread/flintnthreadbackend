package com.ecommerce.authdemo.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelOrderResponseDTO {
    private boolean walletCredited;
    private BigDecimal walletCreditAmount;
    private String message;
}

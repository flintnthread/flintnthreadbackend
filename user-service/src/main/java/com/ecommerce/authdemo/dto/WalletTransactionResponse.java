package com.ecommerce.authdemo.dto;

import com.ecommerce.authdemo.entity.WalletTransaction;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransactionResponse {
    private Integer id;
    private Integer userId;
    private Integer orderId;
    private String orderNumber;
    private BigDecimal amount;
    private WalletTransaction.Type type;
    private String description;
    private Integer createdBy;
    private LocalDateTime createdAt;
}

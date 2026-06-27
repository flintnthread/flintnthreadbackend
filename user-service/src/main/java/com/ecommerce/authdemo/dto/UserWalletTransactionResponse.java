package com.ecommerce.authdemo.dto;

import com.ecommerce.authdemo.entity.UserWalletTransaction;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWalletTransactionResponse {
    private Integer id;
    private Integer orderId;
    private BigDecimal amount;
    private UserWalletTransaction.Type type;
    private String description;
    private LocalDateTime createdAt;
}

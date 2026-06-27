package com.ecommerce.authdemo.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {
    private Integer id;
    private Integer userId;
    private BigDecimal balance;
    private BigDecimal totalEarned;
    private BigDecimal totalSpent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

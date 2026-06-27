package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class EarningsResponse {
    private BigDecimal availableBalance;
    private BigDecimal totalCredits;
    private BigDecimal totalDebits;
    private List<WalletTransactionResponse> transactions;
    private BankAccountResponse bankAccount;
}

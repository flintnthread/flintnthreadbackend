package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.WalletTransactionResponse;

import java.util.List;

public interface WalletTransactionService {
    List<WalletTransactionResponse> getTransactions(Integer userId, Integer orderId);
}

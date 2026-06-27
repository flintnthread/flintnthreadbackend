package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.WalletTransactionResponse;
import com.ecommerce.authdemo.entity.WalletTransaction;
import com.ecommerce.authdemo.repository.WalletTransactionRepository;
import com.ecommerce.authdemo.service.WalletTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletTransactionServiceImpl implements WalletTransactionService {

    private final WalletTransactionRepository walletTransactionRepository;

    @Override
    public List<WalletTransactionResponse> getTransactions(Integer userId, Integer orderId) {
        List<WalletTransaction> rows;

        if (userId != null && orderId != null) {
            rows = walletTransactionRepository.findByUserIdAndOrderId(userId, orderId);
        } else if (userId != null) {
            rows = walletTransactionRepository.findByUserId(userId);
        } else if (orderId != null) {
            rows = walletTransactionRepository.findByOrderId(orderId);
        } else {
            rows = walletTransactionRepository.findAll();
        }

        return rows.stream()
                .sorted(Comparator.comparing(WalletTransaction::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    private WalletTransactionResponse toResponse(WalletTransaction row) {
        return WalletTransactionResponse.builder()
                .id(row.getId())
                .userId(row.getUserId())
                .orderId(row.getOrderId())
                .amount(row.getAmount())
                .type(row.getType())
                .description(row.getDescription())
                .createdBy(row.getCreatedBy())
                .createdAt(row.getCreatedAt())
                .build();
    }
}

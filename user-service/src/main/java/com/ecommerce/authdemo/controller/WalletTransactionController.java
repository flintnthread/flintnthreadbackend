package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.ApiResponse;
import com.ecommerce.authdemo.dto.WalletTransactionResponse;
import com.ecommerce.authdemo.service.WalletTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet-transactions")
@RequiredArgsConstructor
public class WalletTransactionController {

    private final WalletTransactionService walletTransactionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WalletTransactionResponse>>> getTransactions(
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) Integer orderId) {
        List<WalletTransactionResponse> data = walletTransactionService.getTransactions(userId, orderId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Wallet transactions fetched successfully", data));
    }
}

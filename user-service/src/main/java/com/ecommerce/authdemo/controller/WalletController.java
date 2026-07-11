package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.ApiResponse;
import com.ecommerce.authdemo.dto.WalletAmountRequest;
import com.ecommerce.authdemo.dto.WalletResponse;
import com.ecommerce.authdemo.dto.WalletTransactionResponse;
import com.ecommerce.authdemo.exception.OrderException;
import com.ecommerce.authdemo.service.WalletService;
import com.ecommerce.authdemo.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final SecurityUtil securityUtil;

    /** Current user's FNT wallet — creates one if missing. */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet() {
        Integer userId = currentUserIdAsInt();
        WalletResponse data = walletService.getOrCreateWallet(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "FNT Wallet fetched successfully", data));
    }

    @PostMapping("/me/ensure")
    public ResponseEntity<ApiResponse<WalletResponse>> ensureMyWallet() {
        Integer userId = currentUserIdAsInt();
        WalletResponse data = walletService.getOrCreateWallet(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "FNT Wallet ready", data));
    }

    @GetMapping("/me/transactions")
    public ResponseEntity<ApiResponse<List<WalletTransactionResponse>>> getMyTransactions() {
        Integer userId = currentUserIdAsInt();
        walletService.getOrCreateWallet(userId);
        List<WalletTransactionResponse> data = walletService.getTransactionsForUser(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Wallet transactions fetched successfully", data));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(@PathVariable Integer userId) {
        assertCurrentUser(userId);
        WalletResponse data = walletService.getOrCreateWallet(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Wallet fetched successfully", data));
    }

    @PostMapping("/{userId}/create")
    public ResponseEntity<ApiResponse<String>> createWallet(@PathVariable Integer userId) {
        assertCurrentUser(userId);
        walletService.createWallet(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Wallet created successfully", "OK"));
    }

    @PostMapping("/{userId}/credit")
    public ResponseEntity<ApiResponse<String>> addMoney(
            @PathVariable Integer userId,
            @Valid @RequestBody WalletAmountRequest request) {
        assertCurrentUser(userId);
        walletService.addMoney(userId, request.getAmount());
        return ResponseEntity.ok(new ApiResponse<>(true, "Wallet credited successfully", "OK"));
    }

    @PostMapping("/{userId}/debit")
    public ResponseEntity<ApiResponse<String>> deductMoney(
            @PathVariable Integer userId,
            @Valid @RequestBody WalletAmountRequest request) {
        assertCurrentUser(userId);
        if (request.getOrderId() != null && request.getOrderId() > 0) {
            walletService.debitForOrderPayment(
                    userId,
                    request.getOrderId(),
                    BigDecimal.valueOf(request.getAmount()).setScale(2, java.math.RoundingMode.HALF_UP),
                    request.getOrderNumber()
            );
        } else {
            walletService.deductMoney(userId, request.getAmount());
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Wallet debited successfully", "OK"));
    }

    private Integer currentUserIdAsInt() {
        Long userId = securityUtil.getCurrentUserId();
        if (userId == null || userId <= 0) {
            throw new OrderException("Authentication required");
        }
        return Math.toIntExact(userId);
    }

    private void assertCurrentUser(Integer userId) {
        Long current = securityUtil.getCurrentUserId();
        if (current == null || !current.equals(userId.longValue())) {
            throw new OrderException("Access denied");
        }
    }
}

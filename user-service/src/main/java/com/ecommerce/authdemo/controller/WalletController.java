package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.ApiResponse;
import com.ecommerce.authdemo.dto.WalletAmountRequest;
import com.ecommerce.authdemo.dto.WalletRechargeCreateRequest;
import com.ecommerce.authdemo.dto.WalletRechargeVerifyRequest;
import com.ecommerce.authdemo.dto.WalletResponse;
import com.ecommerce.authdemo.dto.WalletTransactionResponse;
import com.ecommerce.authdemo.exception.OrderException;
import com.ecommerce.authdemo.service.RazorpayService;
import com.ecommerce.authdemo.service.WalletService;
import com.ecommerce.authdemo.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final RazorpayService razorpayService;
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

    /**
     * Create a Razorpay order for a fixed wallet top-up (missing amount only).
     * Amount is locked — client must not allow editing.
     */
    @PostMapping("/me/recharge/create")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createWalletRecharge(
            @Valid @RequestBody WalletRechargeCreateRequest request) {
        Integer userId = currentUserIdAsInt();
        walletService.getOrCreateWallet(userId);

        double amount = BigDecimal.valueOf(request.getAmount())
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
        if (amount < 1.0d) {
            throw new OrderException("Recharge amount must be at least ₹1");
        }

        JSONObject order = razorpayService.createOrder(amount);
        Map<String, Object> data = new HashMap<>();
        data.put("razorpayOrderId", order.get("id").toString());
        data.put("amount", order.get("amount")); // paise
        data.put("amountInr", amount);
        data.put("currency", order.has("currency") ? order.get("currency").toString() : razorpayService.getCurrency());
        data.put("status", order.has("status") ? order.get("status").toString() : "created");
        data.put("razorpayKeyId", razorpayService.getPublicKeyId());
        data.put("key", razorpayService.getPublicKeyId());
        data.put("companyName", razorpayService.getCompanyName());
        data.put("editable", false);

        return ResponseEntity.ok(new ApiResponse<>(true, "Wallet recharge order created", data));
    }

    /**
     * Verify Razorpay payment and credit FNT wallet, then return refreshed balance.
     */
    @PostMapping("/me/recharge/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyWalletRecharge(
            @Valid @RequestBody WalletRechargeVerifyRequest request) {
        Integer userId = currentUserIdAsInt();

        boolean ok = razorpayService.verifyPayment(
                request.getRazorpayOrderId(),
                request.getPaymentId(),
                request.getSignature()
        );
        if (!ok) {
            throw new OrderException("Wallet recharge payment verification failed");
        }

        BigDecimal amount = BigDecimal.valueOf(request.getAmount()).setScale(2, RoundingMode.HALF_UP);
        boolean newlyCredited = walletService.creditWalletRecharge(
                userId,
                amount,
                request.getPaymentId(),
                request.getRazorpayOrderId()
        );

        WalletResponse wallet = walletService.getOrCreateWallet(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("wallet", wallet);
        data.put("balance", wallet.getBalance());
        data.put("rechargedAmount", amount);
        data.put("newlyCredited", newlyCredited);
        data.put("razorpayPaymentId", request.getPaymentId());
        data.put("razorpayOrderId", request.getRazorpayOrderId());

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                newlyCredited ? "Wallet recharged successfully" : "Wallet already credited for this payment",
                data
        ));
    }

    /**
     * When UPI/QR succeeded but browser missed handler — confirm Razorpay paid + credit wallet.
     */
    @PostMapping("/me/recharge/confirm-paid")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmWalletRechargePaid(
            @RequestParam String orderId,
            @RequestParam Double amount) {
        Integer userId = currentUserIdAsInt();
        String paymentId = razorpayService.findCapturedPaymentId(orderId);
        if (paymentId == null || paymentId.isBlank()) {
            Map<String, Object> data = new HashMap<>();
            data.put("paid", false);
            return ResponseEntity.ok(new ApiResponse<>(false, "Payment not completed yet", data));
        }

        BigDecimal rechargeAmount = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
        boolean newlyCredited = walletService.creditWalletRecharge(
                userId,
                rechargeAmount,
                paymentId,
                orderId
        );
        WalletResponse wallet = walletService.getOrCreateWallet(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("paid", true);
        data.put("wallet", wallet);
        data.put("balance", wallet.getBalance());
        data.put("rechargedAmount", rechargeAmount);
        data.put("newlyCredited", newlyCredited);
        data.put("razorpayPaymentId", paymentId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Wallet recharged successfully", data));
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

package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.WalletResponse;
import com.ecommerce.authdemo.dto.WalletTransactionResponse;

import java.math.BigDecimal;
import java.util.List;

public interface WalletService {

    void createWallet(Integer userId);

    WalletResponse getWallet(Integer userId);

    WalletResponse getOrCreateWallet(Integer userId);

    void addMoney(Integer userId, Double amount);

    /**
     * Credit wallet after Razorpay recharge (idempotent per razorpayPaymentId).
     * @return true if newly credited, false if already credited for this payment
     */
    boolean creditWalletRecharge(
            Integer userId,
            BigDecimal amount,
            String razorpayPaymentId,
            String razorpayOrderId
    );

    void deductMoney(Integer userId, Double amount);

    /** Credit FNT wallet when a prepaid order is cancelled (idempotent per order). */
    boolean creditOrderCancellationRefund(
            Integer userId,
            Long orderId,
            BigDecimal amount,
            String orderNumber
    );

    /** Credit FNT wallet when a prepaid return/refund is processed (idempotent per return). */
    boolean creditOrderReturnRefund(
            Integer userId,
            Long orderId,
            Long returnId,
            BigDecimal amount,
            String orderNumber
    );

    /** Debit FNT wallet when customer pays with wallet at checkout. */
    void debitForOrderPayment(
            Integer userId,
            Long orderId,
            BigDecimal amount,
            String orderNumber
    );

    List<WalletTransactionResponse> getTransactionsForUser(Integer userId);

    /** Sum of wallet debits for a specific order (checkout payment). */
    BigDecimal getWalletDebitTotalForOrder(Integer userId, Long orderId);

    /** Returns amount already credited for a cancelled order refund, if any. */
    java.util.Optional<BigDecimal> findOrderCancellationRefundAmount(Integer userId, Long orderId);
}

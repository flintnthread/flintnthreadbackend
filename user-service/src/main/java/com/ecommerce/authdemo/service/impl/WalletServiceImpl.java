package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.WalletResponse;
import com.ecommerce.authdemo.dto.WalletTransactionResponse;
import com.ecommerce.authdemo.entity.UserWallet;
import com.ecommerce.authdemo.entity.WalletTransaction;
import com.ecommerce.authdemo.exception.ResourceNotFoundException;
import com.ecommerce.authdemo.repository.UserWalletRepository;
import com.ecommerce.authdemo.repository.WalletTransactionRepository;
import com.ecommerce.authdemo.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private static final String CANCEL_REFUND_DESC_PREFIX = "FNT_ORDER_CANCEL_REFUND:";
    private static final String RETURN_REFUND_DESC_PREFIX = "FNT_ORDER_RETURN_REFUND:";

    private final UserWalletRepository walletRepo;
    private final WalletTransactionRepository walletTransactionRepo;

    @Override
    @Transactional
    public void createWallet(Integer userId) {
        if (walletRepo.findByUserId(userId).isPresent()) {
            return;
        }

        UserWallet wallet = UserWallet.builder()
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .totalEarned(BigDecimal.ZERO)
                .totalSpent(BigDecimal.ZERO)
                .build();

        walletRepo.save(wallet);
    }

    @Override
    public WalletResponse getWallet(Integer userId) {
        UserWallet wallet = walletRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        return toResponse(wallet);
    }

    @Override
    @Transactional
    public WalletResponse getOrCreateWallet(Integer userId) {
        createWallet(userId);
        return getWallet(userId);
    }

    @Override
    @Transactional
    public void addMoney(Integer userId, Double amount) {
        BigDecimal value = normalizeAmount(amount);
        UserWallet wallet = requireWallet(userId);

        wallet.setBalance(safe(wallet.getBalance()).add(value));
        wallet.setTotalEarned(safe(wallet.getTotalEarned()).add(value));
        walletRepo.save(wallet);

        walletTransactionRepo.save(
                WalletTransaction.builder()
                        .userId(userId)
                        .amount(value)
                        .type(WalletTransaction.Type.credit)
                        .description("FNT Wallet credited")
                        .createdBy(userId)
                        .build()
        );
    }

    @Override
    @Transactional
    public void deductMoney(Integer userId, Double amount) {
        BigDecimal value = normalizeAmount(amount);
        UserWallet wallet = requireWallet(userId);

        if (safe(wallet.getBalance()).compareTo(value) < 0) {
            throw new RuntimeException("Insufficient FNT wallet balance");
        }

        wallet.setBalance(safe(wallet.getBalance()).subtract(value));
        wallet.setTotalSpent(safe(wallet.getTotalSpent()).add(value));
        walletRepo.save(wallet);

        walletTransactionRepo.save(
                WalletTransaction.builder()
                        .userId(userId)
                        .amount(value)
                        .type(WalletTransaction.Type.debit)
                        .description("FNT Wallet debited")
                        .createdBy(userId)
                        .build()
        );
    }

    @Override
    @Transactional
    public boolean creditOrderCancellationRefund(
            Integer userId,
            Long orderId,
            BigDecimal amount,
            String orderNumber
    ) {
        BigDecimal value = normalizeAmount(amount);
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        String description = CANCEL_REFUND_DESC_PREFIX + orderId;
        if (walletTransactionRepo.existsByUserIdAndDescription(userId, description)) {
            return false;
        }

        createWallet(userId);
        UserWallet wallet = requireWallet(userId);

        wallet.setBalance(safe(wallet.getBalance()).add(value));
        wallet.setTotalEarned(safe(wallet.getTotalEarned()).add(value));
        walletRepo.save(wallet);

        walletTransactionRepo.save(
                WalletTransaction.builder()
                        .userId(userId)
                        .orderId(orderId != null ? Math.toIntExact(orderId) : null)
                        .amount(value)
                        .type(WalletTransaction.Type.credit)
                        .description(description)
                        .createdBy(userId)
                        .build()
        );

        return true;
    }

    @Override
    @Transactional
    public boolean creditOrderReturnRefund(
            Integer userId,
            Long orderId,
            Long returnId,
            BigDecimal amount,
            String orderNumber
    ) {
        BigDecimal value = normalizeAmount(amount);
        if (value.compareTo(BigDecimal.ZERO) <= 0 || returnId == null) {
            return false;
        }

        String description = RETURN_REFUND_DESC_PREFIX + returnId;
        if (walletTransactionRepo.existsByUserIdAndDescription(userId, description)) {
            return false;
        }

        createWallet(userId);
        UserWallet wallet = requireWallet(userId);

        wallet.setBalance(safe(wallet.getBalance()).add(value));
        wallet.setTotalEarned(safe(wallet.getTotalEarned()).add(value));
        walletRepo.save(wallet);

        walletTransactionRepo.save(
                WalletTransaction.builder()
                        .userId(userId)
                        .orderId(orderId != null ? Math.toIntExact(orderId) : null)
                        .amount(value)
                        .type(WalletTransaction.Type.credit)
                        .description(description)
                        .createdBy(userId)
                        .build()
        );

        return true;
    }

    @Override
    @Transactional
    public void debitForOrderPayment(
            Integer userId,
            Long orderId,
            BigDecimal amount,
            String orderNumber
    ) {
        BigDecimal value = normalizeAmount(amount);
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        createWallet(userId);
        UserWallet wallet = requireWallet(userId);

        if (safe(wallet.getBalance()).compareTo(value) < 0) {
            throw new RuntimeException("Insufficient FNT wallet balance");
        }

        wallet.setBalance(safe(wallet.getBalance()).subtract(value));
        wallet.setTotalSpent(safe(wallet.getTotalSpent()).add(value));
        walletRepo.save(wallet);

        String label = orderNumber != null && !orderNumber.isBlank()
                ? orderNumber
                : String.valueOf(orderId);

        walletTransactionRepo.save(
                WalletTransaction.builder()
                        .userId(userId)
                        .orderId(orderId != null ? Math.toIntExact(orderId) : null)
                        .amount(value)
                        .type(WalletTransaction.Type.debit)
                        .description("FNT Wallet used for order " + label)
                        .createdBy(userId)
                        .build()
        );
    }

    @Override
    public BigDecimal getWalletDebitTotalForOrder(Integer userId, Long orderId) {
        if (userId == null || orderId == null || orderId <= 0 || orderId > Integer.MAX_VALUE) {
            return BigDecimal.ZERO;
        }
        int orderIdInt = orderId.intValue();
        return walletTransactionRepo.findByUserIdAndOrderId(userId, orderIdInt).stream()
                .filter(txn -> txn.getType() == WalletTransaction.Type.debit)
                .map(WalletTransaction::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public Optional<BigDecimal> findOrderCancellationRefundAmount(Integer userId, Long orderId) {
        if (userId == null || orderId == null || orderId <= 0) {
            return Optional.empty();
        }
        String description = CANCEL_REFUND_DESC_PREFIX + orderId;
        return walletTransactionRepo
                .findFirstByUserIdAndDescription(userId, description)
                .map(WalletTransaction::getAmount)
                .filter(amount -> amount != null && amount.compareTo(BigDecimal.ZERO) > 0)
                .map(amount -> amount.setScale(2, RoundingMode.HALF_UP));
    }

    @Override
    public List<WalletTransactionResponse> getTransactionsForUser(Integer userId) {
        return walletTransactionRepo.findByUserId(userId).stream()
                .sorted(Comparator.comparing(
                        WalletTransaction::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(this::toTransactionResponse)
                .toList();
    }

    private UserWallet requireWallet(Integer userId) {
        return walletRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
    }

    private BigDecimal normalizeAmount(Double amount) {
        if (amount == null || amount <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private WalletResponse toResponse(UserWallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .totalEarned(wallet.getTotalEarned())
                .totalSpent(wallet.getTotalSpent())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    private WalletTransactionResponse toTransactionResponse(WalletTransaction row) {
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

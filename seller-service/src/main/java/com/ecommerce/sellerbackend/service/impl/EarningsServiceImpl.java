package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.dto.BankAccountResponse;
import com.ecommerce.sellerbackend.dto.EarningsResponse;
import com.ecommerce.sellerbackend.dto.OrderPayoutAmountDto;
import com.ecommerce.sellerbackend.dto.PayoutRequestBody;
import com.ecommerce.sellerbackend.dto.PayoutRequestResponse;
import com.ecommerce.sellerbackend.dto.PayoutTransactionResponse;
import com.ecommerce.sellerbackend.dto.WalletTransactionResponse;
import com.ecommerce.sellerbackend.entity.Order;
import com.ecommerce.sellerbackend.entity.Seller;
import com.ecommerce.sellerbackend.entity.WalletTransaction;
import com.ecommerce.sellerbackend.exception.ResourceNotFoundException;
import com.ecommerce.sellerbackend.repository.OrderItemRepository;
import com.ecommerce.sellerbackend.repository.OrderRepository;
import com.ecommerce.sellerbackend.repository.SellerRepository;
import com.ecommerce.sellerbackend.repository.WalletTransactionRepository;
import com.ecommerce.sellerbackend.service.EarningsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EarningsServiceImpl implements EarningsService {

    private static final NumberFormat INR = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private final SellerRepository sellerRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional(readOnly = true)
    public EarningsResponse getEarnings(Long sellerId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found."));
        List<WalletTransaction> rows = walletTransactionRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);

        BigDecimal credits = BigDecimal.ZERO;
        BigDecimal debits = BigDecimal.ZERO;
        List<WalletTransactionResponse> transactions = new ArrayList<>();

        for (WalletTransaction row : rows) {
            if (row.getType() == WalletTransaction.TxType.credit) {
                credits = credits.add(row.getAmount());
            } else {
                debits = debits.add(row.getAmount());
            }
            transactions.add(WalletTransactionResponse.builder()
                    .id(row.getId())
                    .title(row.getDescription() != null ? row.getDescription() : "Transaction")
                    .amount(formatSigned(row.getType(), row.getAmount()))
                    .date(row.getCreatedAt() != null ? row.getCreatedAt().format(DISPLAY_DATE) : "")
                    .status("Completed")
                    .type(row.getType() != null ? row.getType().name() : "credit")
                    .orderId(row.getOrderId())
                    .build());
        }

        BankAccountResponse bank = BankAccountResponse.builder()
                .bankName(seller.getBankName())
                .accountNumberMasked(maskAccount(seller.getAccountNumber()))
                .ifscCode(seller.getIfscCode())
                .accountHolder(seller.getAccountHolder())
                .verified(Boolean.TRUE.equals(seller.getBankVerified()))
                .build();

        return EarningsResponse.builder()
                .availableBalance(seller.getWalletBalance())
                .totalCredits(credits)
                .totalDebits(debits)
                .transactions(transactions)
                .bankAccount(bank)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayoutTransactionResponse> getPayouts(Long sellerId) {
        return walletTransactionRepository.findBySellerIdOrderByCreatedAtDesc(sellerId).stream()
                .filter(t -> t.getType() == WalletTransaction.TxType.debit)
                .map(t -> PayoutTransactionResponse.builder()
                        .id(String.valueOf(t.getId()))
                        .orderId(t.getOrderId() != null ? "ORD" + t.getOrderId() : "—")
                        .amount(t.getAmount())
                        .date(t.getCreatedAt() != null ? t.getCreatedAt().toString() : "")
                        .status("Completed")
                        .type("Payout")
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public PayoutRequestResponse requestPayout(Long sellerId, PayoutRequestBody body) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found."));
        if (body.getAmount() == null || body.getAmount().compareTo(BigDecimal.ONE) < 0) {
            throw new IllegalArgumentException("Minimum payout amount is ₹1.");
        }
        if (body.getOtp() == null || body.getOtp().trim().length() < 4) {
            throw new IllegalArgumentException("Valid OTP is required.");
        }
        BigDecimal balance = seller.getWalletBalance() != null ? seller.getWalletBalance() : BigDecimal.ZERO;
        if (balance.compareTo(body.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance.");
        }

        Long orderId = null;
        if (body.getOrderId() != null && !body.getOrderId().isBlank()) {
            orderId = resolveOrderId(sellerId, body.getOrderId().trim());
            if (orderId == null) {
                throw new IllegalArgumentException("Order not found for this seller.");
            }
            BigDecimal orderTotal = orderItemRepository.sumTotalForSellerOrder(sellerId, orderId);
            if (orderTotal == null || orderTotal.compareTo(body.getAmount()) < 0) {
                throw new IllegalArgumentException("Payout amount exceeds eligible order earnings.");
            }
        }

        WalletTransaction tx = new WalletTransaction();
        tx.setSellerId(sellerId);
        tx.setOrderId(orderId);
        tx.setAmount(body.getAmount());
        tx.setType(WalletTransaction.TxType.debit);
        String desc = body.getDescription() != null && !body.getDescription().isBlank()
                ? body.getDescription().trim()
                : "Payout request";
        if (orderId != null) {
            desc = desc + " (Order " + body.getOrderId().trim() + ")";
        }
        tx.setDescription(desc);
        tx.setCreatedAt(java.time.LocalDateTime.now());
        tx.setCreatedBy(sellerId);
        walletTransactionRepository.save(tx);

        seller.setWalletBalance(balance.subtract(body.getAmount()));
        sellerRepository.save(seller);

        return PayoutRequestResponse.builder()
                .transactionId(String.valueOf(tx.getId()))
                .amount(body.getAmount())
                .remainingBalance(seller.getWalletBalance())
                .status("Pending")
                .message("Payout request submitted successfully.")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderPayoutAmountDto lookupOrderPayoutAmount(Long sellerId, String orderKey) {
        Long orderId = resolveOrderId(sellerId, orderKey);
        if (orderId == null) {
            return OrderPayoutAmountDto.builder()
                    .orderKey(orderKey)
                    .found(false)
                    .amount(BigDecimal.ZERO)
                    .build();
        }
        BigDecimal amount = orderItemRepository.sumTotalForSellerOrder(sellerId, orderId);
        return OrderPayoutAmountDto.builder()
                .orderKey(orderKey)
                .orderId(orderId)
                .amount(amount != null ? amount : BigDecimal.ZERO)
                .found(true)
                .build();
    }

    private Long resolveOrderId(Long sellerId, String orderKey) {
        String key = orderKey.trim();
        if (key.matches("\\d+")) {
            long numericId = Long.parseLong(key);
            if (!orderItemRepository.findBySellerIdAndOrderId(sellerId, numericId).isEmpty()) {
                return numericId;
            }
        }
        String stripped = key.toUpperCase();
        if (stripped.startsWith("ORD")) {
            stripped = stripped.substring(3);
        }
        if (stripped.startsWith("FNT")) {
            stripped = stripped.substring(3);
        }
        final String orderNumberAlt = "ORD" + stripped;
        return orderRepository.findByOrderNumber(key)
                .filter(o -> !orderItemRepository.findBySellerIdAndOrderId(sellerId, o.getId()).isEmpty())
                .map(Order::getId)
                .or(() -> orderRepository.findByOrderNumber(orderNumberAlt)
                        .filter(o -> !orderItemRepository.findBySellerIdAndOrderId(sellerId, o.getId()).isEmpty())
                        .map(Order::getId))
                .orElse(null);
    }




    private String formatSigned(WalletTransaction.TxType type, BigDecimal amount) {
        String formatted = INR.format(amount != null ? amount : BigDecimal.ZERO);
        return type == WalletTransaction.TxType.debit ? "-" + formatted : formatted;
    }

    private String maskAccount(String account) {
        if (account == null || account.length() < 4) {
            return "••••";
        }
        return "•••• " + account.substring(account.length() - 4);
    }
}

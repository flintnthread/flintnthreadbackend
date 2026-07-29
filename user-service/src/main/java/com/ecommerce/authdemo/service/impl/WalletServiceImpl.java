package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.WalletResponse;
import com.ecommerce.authdemo.dto.WalletTransactionResponse;
import com.ecommerce.authdemo.entity.UserWallet;
import com.ecommerce.authdemo.entity.UserWalletTransaction;
import com.ecommerce.authdemo.entity.WalletTransaction;
import com.ecommerce.authdemo.exception.ResourceNotFoundException;
import com.ecommerce.authdemo.repository.OrderRepository;
import com.ecommerce.authdemo.repository.UserWalletRepository;
import com.ecommerce.authdemo.repository.UserWalletTransactionRepository;
import com.ecommerce.authdemo.repository.WalletTransactionRepository;
import com.ecommerce.authdemo.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private static final String CANCEL_REFUND_DESC_PREFIX = "FNT_ORDER_CANCEL_REFUND:";
    private static final String RETURN_REFUND_DESC_PREFIX = "FNT_ORDER_RETURN_REFUND:";

    private final UserWalletRepository walletRepo;
    private final WalletTransactionRepository walletTransactionRepo;
    private final UserWalletTransactionRepository userWalletTransactionRepo;
    private final OrderRepository orderRepository;
    private final JdbcTemplate jdbcTemplate;

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

        persistLedgerRow(
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
    public boolean creditWalletRecharge(
            Integer userId,
            BigDecimal amount,
            String razorpayPaymentId,
            String razorpayOrderId
    ) {
        BigDecimal value = normalizeAmount(amount);
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Recharge amount must be greater than zero");
        }
        if (razorpayPaymentId == null || razorpayPaymentId.isBlank()) {
            throw new RuntimeException("Razorpay payment id is required for wallet recharge");
        }

        String description = "FNT_WALLET_RECHARGE:" + razorpayPaymentId.trim();
        if (walletTransactionRepo.existsByUserIdAndDescriptionStartingWith(userId, description)) {
            return false;
        }

        createWallet(userId);
        UserWallet wallet = requireWallet(userId);

        wallet.setBalance(safe(wallet.getBalance()).add(value));
        wallet.setTotalEarned(safe(wallet.getTotalEarned()).add(value));
        walletRepo.save(wallet);

        String label = razorpayOrderId != null && !razorpayOrderId.isBlank()
                ? " (Razorpay " + razorpayOrderId.trim() + ")"
                : "";
        persistLedgerRow(
                WalletTransaction.builder()
                        .userId(userId)
                        .amount(value)
                        .type(WalletTransaction.Type.credit)
                        .description(description + " | Wallet Recharge via Razorpay" + label)
                        .createdBy(userId)
                        .build()
        );
        return true;
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

        persistLedgerRow(
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

        persistLedgerRow(
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

        persistLedgerRow(
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

        persistLedgerRow(
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
        if (userId == null || userId <= 0) {
            return List.of();
        }

        Map<String, WalletTransactionResponse> merged = new LinkedHashMap<>();

        for (WalletTransactionResponse row : loadPrimaryWalletTransactions(userId)) {
            merged.putIfAbsent(transactionDedupeKey(row), row);
        }
        for (WalletTransactionResponse row : loadLegacyUserWalletTransactions(userId)) {
            merged.putIfAbsent(transactionDedupeKey(row), row);
        }
        for (WalletTransactionResponse row : loadWalletTransactionsViaJdbc(userId)) {
            merged.putIfAbsent(transactionDedupeKey(row), row);
        }

        if (merged.isEmpty()) {
            for (WalletTransactionResponse row : reconstructTransactionsFromOrders(userId)) {
                merged.putIfAbsent(transactionDedupeKey(row), row);
            }
        }

        return merged.values().stream()
                .sorted(Comparator.comparing(
                        WalletTransactionResponse::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();
    }

    private List<WalletTransactionResponse> loadPrimaryWalletTransactions(Integer userId) {
        try {
            return walletTransactionRepo.findByUserId(userId).stream()
                    .map(this::toTransactionResponse)
                    .toList();
        } catch (Exception e) {
            log.warn("[WALLET] JPA wallet_transactions lookup failed userId={}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    private List<WalletTransactionResponse> loadLegacyUserWalletTransactions(Integer userId) {
        try {
            return userWalletTransactionRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                    .map(this::toTransactionResponse)
                    .toList();
        } catch (Exception e) {
            log.warn("[WALLET] user_wallet_transactions lookup failed userId={}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Fallback when the owner column is seller_id or user_id and JPA mapping misses rows.
     */
    private List<WalletTransactionResponse> loadWalletTransactionsViaJdbc(Integer userId) {
        List<WalletTransactionResponse> out = new ArrayList<>();
        for (String ownerColumn : List.of("seller_id", "user_id")) {
            if (!walletTxnColumnExists(ownerColumn)) {
                continue;
            }
            try {
                String sql =
                        "SELECT id, `" + ownerColumn + "` AS owner_id, order_id, amount, type, description, created_by, created_at "
                                + "FROM wallet_transactions WHERE `" + ownerColumn + "` = ? "
                                + "ORDER BY created_at DESC, id DESC";
                out.addAll(jdbcTemplate.query(sql, (rs, rowNum) -> mapJdbcWalletTxn(rs), userId));
            } catch (Exception e) {
                log.warn(
                        "[WALLET] JDBC wallet_transactions lookup failed userId={} col={}: {}",
                        userId,
                        ownerColumn,
                        e.getMessage()
                );
            }
        }
        return out;
    }

    private WalletTransactionResponse mapJdbcWalletTxn(java.sql.ResultSet rs) throws java.sql.SQLException {
        String typeRaw = rs.getString("type");
        WalletTransaction.Type type =
                typeRaw != null && typeRaw.equalsIgnoreCase("debit")
                        ? WalletTransaction.Type.debit
                        : WalletTransaction.Type.credit;
        Timestamp createdTs = rs.getTimestamp("created_at");
        LocalDateTime createdAt = createdTs != null ? createdTs.toLocalDateTime() : null;
        Integer orderId = rs.getObject("order_id") != null ? rs.getInt("order_id") : null;
        String orderNumber = null;
        if (orderId != null && orderId > 0) {
            orderNumber = orderRepository.findById(orderId.longValue())
                    .map(order -> order.getOrderNumber())
                    .filter(num -> num != null && !num.isBlank())
                    .orElse(null);
        }
        return WalletTransactionResponse.builder()
                .id(rs.getInt("id"))
                .userId(rs.getInt("owner_id"))
                .orderId(orderId)
                .orderNumber(orderNumber)
                .amount(rs.getBigDecimal("amount"))
                .type(type)
                .description(rs.getString("description"))
                .createdBy(rs.getObject("created_by") != null ? rs.getInt("created_by") : null)
                .createdAt(createdAt)
                .build();
    }

    private boolean walletTxnColumnExists(String column) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'wallet_transactions'
                      AND COLUMN_NAME = ?
                    """,
                    Integer.class,
                    column
            );
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * When ledger tables are empty but wallet totals exist, rebuild visible history from orders
     * (checkout wallet usage + wallet balance summary credits).
     */
    private List<WalletTransactionResponse> reconstructTransactionsFromOrders(Integer userId) {
        List<WalletTransactionResponse> rows = new ArrayList<>();
        BigDecimal debitSum = BigDecimal.ZERO;
        try {
            List<com.ecommerce.authdemo.entity.Order> orders =
                    orderRepository.findByUserIdOrderByCreatedAtDesc(userId.longValue());
            int synthId = -1;
            for (com.ecommerce.authdemo.entity.Order order : orders) {
                if (order == null) continue;
                Double walletUsed = order.getWalletAmountUsed();
                if (walletUsed == null || walletUsed <= 0.009d) continue;
                BigDecimal amount = BigDecimal.valueOf(walletUsed).setScale(2, RoundingMode.HALF_UP);
                debitSum = debitSum.add(amount);
                rows.add(WalletTransactionResponse.builder()
                        .id(synthId--)
                        .userId(userId)
                        .orderId(order.getId() != null ? Math.toIntExact(order.getId()) : null)
                        .orderNumber(order.getOrderNumber())
                        .amount(amount)
                        .type(WalletTransaction.Type.debit)
                        .description("Used at checkout"
                                + (order.getOrderNumber() != null && !order.getOrderNumber().isBlank()
                                ? " · " + order.getOrderNumber().trim()
                                : ""))
                        .createdBy(userId)
                        .createdAt(order.getCreatedAt())
                        .build());
            }
        } catch (Exception e) {
            log.warn("[WALLET] order-based history rebuild failed userId={}: {}", userId, e.getMessage());
        }

        try {
            UserWallet wallet = walletRepo.findByUserId(userId).orElse(null);
            if (wallet == null) {
                return rows;
            }
            BigDecimal totalEarned = safe(wallet.getTotalEarned());
            BigDecimal totalSpent = safe(wallet.getTotalSpent());
            BigDecimal balance = safe(wallet.getBalance());

            BigDecimal remainingDebit = totalSpent.subtract(debitSum);
            if (remainingDebit.compareTo(new BigDecimal("0.01")) >= 0) {
                rows.add(WalletTransactionResponse.builder()
                        .id(-900001)
                        .userId(userId)
                        .amount(remainingDebit.setScale(2, RoundingMode.HALF_UP))
                        .type(WalletTransaction.Type.debit)
                        .description("Wallet used on earlier checkouts")
                        .createdBy(userId)
                        .createdAt(wallet.getUpdatedAt() != null ? wallet.getUpdatedAt() : wallet.getCreatedAt())
                        .build());
            }

            if (totalEarned.compareTo(new BigDecimal("0.01")) >= 0) {
                rows.add(WalletTransactionResponse.builder()
                        .id(-900002)
                        .userId(userId)
                        .amount(totalEarned.setScale(2, RoundingMode.HALF_UP))
                        .type(WalletTransaction.Type.credit)
                        .description("Refunds & wallet credits")
                        .createdBy(userId)
                        .createdAt(wallet.getCreatedAt())
                        .build());
            } else if (rows.isEmpty() && balance.compareTo(new BigDecimal("0.01")) >= 0) {
                rows.add(WalletTransactionResponse.builder()
                        .id(-900003)
                        .userId(userId)
                        .amount(balance.setScale(2, RoundingMode.HALF_UP))
                        .type(WalletTransaction.Type.credit)
                        .description("Available FNT Wallet balance")
                        .createdBy(userId)
                        .createdAt(wallet.getUpdatedAt() != null ? wallet.getUpdatedAt() : wallet.getCreatedAt())
                        .build());
            }
        } catch (Exception e) {
            log.warn("[WALLET] wallet-summary history rebuild failed userId={}: {}", userId, e.getMessage());
        }
        return rows;
    }

    private String transactionDedupeKey(WalletTransactionResponse row) {
        if (row == null) return "null";
        if (row.getId() != null && row.getId() > 0) {
            return "id:" + row.getId() + ":" + (row.getType() != null ? row.getType().name() : "");
        }
        return String.join(
                "|",
                String.valueOf(row.getAmount()),
                row.getType() != null ? row.getType().name() : "",
                String.valueOf(row.getOrderId()),
                String.valueOf(row.getDescription()),
                String.valueOf(row.getCreatedAt())
        );
    }

    private WalletTransactionResponse toTransactionResponse(UserWalletTransaction row) {
        String orderNumber = null;
        if (row.getOrderId() != null && row.getOrderId() > 0) {
            orderNumber = orderRepository.findById(row.getOrderId().longValue())
                    .map(order -> order.getOrderNumber())
                    .filter(num -> num != null && !num.isBlank())
                    .orElse(null);
        }
        if (orderNumber == null) {
            String desc = row.getDescription() != null ? row.getDescription() : "";
            java.util.regex.Matcher matcher =
                    java.util.regex.Pattern.compile("(FNT\\d{10,})").matcher(desc);
            if (matcher.find()) {
                orderNumber = matcher.group(1);
            }
        }
        WalletTransaction.Type type =
                row.getType() == UserWalletTransaction.Type.debit
                        ? WalletTransaction.Type.debit
                        : WalletTransaction.Type.credit;
        return WalletTransactionResponse.builder()
                .id(row.getId())
                .userId(row.getUserId())
                .orderId(row.getOrderId())
                .orderNumber(orderNumber)
                .amount(row.getAmount())
                .type(type)
                .description(row.getDescription())
                .createdBy(row.getUserId())
                .createdAt(row.getCreatedAt())
                .build();
    }

    /** Persist ledger row to both tables so history stays available across schema variants. */
    private void persistLedgerRow(WalletTransaction row) {
        walletTransactionRepo.save(row);
        try {
            userWalletTransactionRepo.save(
                    UserWalletTransaction.builder()
                            .userId(row.getUserId())
                            .orderId(row.getOrderId())
                            .amount(row.getAmount())
                            .type(
                                    row.getType() == WalletTransaction.Type.debit
                                            ? UserWalletTransaction.Type.debit
                                            : UserWalletTransaction.Type.credit
                            )
                            .description(row.getDescription())
                            .build()
            );
        } catch (Exception e) {
            log.warn(
                    "[WALLET] Mirror write to user_wallet_transactions failed userId={}: {}",
                    row.getUserId(),
                    e.getMessage()
            );
        }
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
        String orderNumber = null;
        if (row.getOrderId() != null && row.getOrderId() > 0) {
            orderNumber = orderRepository.findById(row.getOrderId().longValue())
                    .map(order -> order.getOrderNumber())
                    .filter(num -> num != null && !num.isBlank())
                    .orElse(null);
        }
        if (orderNumber == null) {
            String desc = row.getDescription() != null ? row.getDescription() : "";
            java.util.regex.Matcher matcher =
                    java.util.regex.Pattern.compile("(FNT\\d{10,})").matcher(desc);
            if (matcher.find()) {
                orderNumber = matcher.group(1);
            }
        }
        return WalletTransactionResponse.builder()
                .id(row.getId())
                .userId(row.getUserId())
                .orderId(row.getOrderId())
                .orderNumber(orderNumber)
                .amount(row.getAmount())
                .type(row.getType())
                .description(row.getDescription())
                .createdBy(row.getCreatedBy())
                .createdAt(row.getCreatedAt())
                .build();
    }
}

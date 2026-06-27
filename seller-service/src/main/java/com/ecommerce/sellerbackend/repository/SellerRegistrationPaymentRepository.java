package com.ecommerce.sellerbackend.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class SellerRegistrationPaymentRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void ensureTable() {
        entityManager.createNativeQuery("""
                CREATE TABLE IF NOT EXISTS seller_registration_payments (
                  seller_id BIGINT NOT NULL PRIMARY KEY,
                  amount INT NOT NULL,
                  currency VARCHAR(8) NOT NULL,
                  razorpay_order_id VARCHAR(80),
                  razorpay_payment_id VARCHAR(80),
                  razorpay_signature VARCHAR(255),
                  display_order_number VARCHAR(32),
                  invoice_number VARCHAR(32),
                  status VARCHAR(20) NOT NULL DEFAULT 'pending',
                  receipt VARCHAR(80),
                  paid_at DATETIME NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """).executeUpdate();
        addColumnIfMissing("amount", "INT NOT NULL DEFAULT 19900");
        addColumnIfMissing("currency", "VARCHAR(8) NOT NULL DEFAULT 'INR'");
        addColumnIfMissing("razorpay_order_id", "VARCHAR(80)");
        addColumnIfMissing("razorpay_payment_id", "VARCHAR(80)");
        addColumnIfMissing("razorpay_signature", "VARCHAR(255)");
        addColumnIfMissing("display_order_number", "VARCHAR(32)");
        addColumnIfMissing("invoice_number", "VARCHAR(32)");
        addColumnIfMissing("status", "VARCHAR(20) NOT NULL DEFAULT 'pending'");
        addColumnIfMissing("receipt", "VARCHAR(80)");
        addColumnIfMissing("paid_at", "DATETIME NULL");
        addColumnIfMissing("created_at", "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
        addColumnIfMissing("updated_at", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
        migrateLegacyColumns();
    }

    private void migrateLegacyColumns() {
        if (!columnExists("order_id")) {
            return;
        }
        if (columnExists("razorpay_order_id")) {
            entityManager.createNativeQuery("""
                    UPDATE seller_registration_payments
                    SET razorpay_order_id = order_id
                    WHERE razorpay_order_id IS NULL
                      AND order_id IS NOT NULL
                    """).executeUpdate();
        }
        if (columnExists("payment_id")) {
            entityManager.createNativeQuery("""
                    UPDATE seller_registration_payments
                    SET razorpay_payment_id = payment_id
                    WHERE razorpay_payment_id IS NULL
                      AND payment_id IS NOT NULL
                    """).executeUpdate();
        }
        if (columnExists("payment_signature")) {
            entityManager.createNativeQuery("""
                    UPDATE seller_registration_payments
                    SET razorpay_signature = payment_signature
                    WHERE razorpay_signature IS NULL
                      AND payment_signature IS NOT NULL
                    """).executeUpdate();
        }
    }

    private void addColumnIfMissing(String columnName, String sqlDefinition) {
        if (columnExists(columnName)) {
            return;
        }
        entityManager.createNativeQuery(
                "ALTER TABLE seller_registration_payments ADD COLUMN " + columnName + " " + sqlDefinition
        ).executeUpdate();
    }

    private boolean columnExists(String columnName) {
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(1)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'seller_registration_payments'
                  AND column_name = :columnName
                """)
                .setParameter("columnName", columnName)
                .getSingleResult();
        return count != null && count.longValue() > 0;
    }

    @SuppressWarnings("unchecked")
    public PaymentRecord findBySellerId(Long sellerId) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT seller_id,
                       amount,
                       currency,
                       razorpay_order_id,
                       razorpay_payment_id,
                       razorpay_signature,
                       display_order_number,
                       invoice_number,
                       status,
                       receipt,
                       paid_at,
                       created_at
                FROM seller_registration_payments
                WHERE seller_id = :sellerId
                LIMIT 1
                """)
                .setParameter("sellerId", sellerId)
                .getResultList();
        if (rows.isEmpty()) {
            return null;
        }
        Object[] row = rows.get(0);
        PaymentRecord record = new PaymentRecord();
        record.setSellerId(((Number) row[0]).longValue());
        record.setAmount(((Number) row[1]).intValue());
        record.setCurrency(row[2] != null ? row[2].toString() : null);
        record.setOrderId(row[3] != null ? row[3].toString() : null);
        record.setPaymentId(row[4] != null ? row[4].toString() : null);
        record.setPaymentSignature(row[5] != null ? row[5].toString() : null);
        record.setDisplayOrderNumber(row[6] != null ? row[6].toString() : null);
        record.setInvoiceNumber(row[7] != null ? row[7].toString() : null);
        record.setStatus(row[8] != null ? row[8].toString() : null);
        record.setReceipt(row[9] != null ? row[9].toString() : null);
        if (row[10] instanceof Timestamp ts) {
            record.setPaidAt(ts.toLocalDateTime());
        }
        if (row[11] instanceof Timestamp createdAt) {
            record.setCreatedAt(createdAt.toLocalDateTime());
        }
        return record;
    }

    @Transactional
    public void saveOrUpdateOrder(
            Long sellerId,
            int amount,
            String currency,
            String razorpayOrderId,
            String receipt,
            String displayOrderNumber) {
        int updated = entityManager.createNativeQuery("""
                UPDATE seller_registration_payments
                SET amount = :amount,
                    currency = :currency,
                    razorpay_order_id = :orderId,
                    receipt = :receipt,
                    display_order_number = :displayOrderNumber,
                    status = 'pending',
                    razorpay_payment_id = NULL,
                    razorpay_signature = NULL,
                    invoice_number = NULL,
                    paid_at = NULL
                WHERE seller_id = :sellerId
                """)
                .setParameter("sellerId", sellerId)
                .setParameter("amount", amount)
                .setParameter("currency", currency)
                .setParameter("orderId", razorpayOrderId)
                .setParameter("receipt", receipt)
                .setParameter("displayOrderNumber", displayOrderNumber)
                .executeUpdate();
        if (updated == 0) {
            entityManager.createNativeQuery("""
                    INSERT INTO seller_registration_payments
                    (
                      seller_id,
                      amount,
                      currency,
                      razorpay_order_id,
                      receipt,
                      display_order_number,
                      status,
                      created_at,
                      updated_at
                    )
                    VALUES (:sellerId, :amount, :currency, :orderId, :receipt, :displayOrderNumber, 'pending', NOW(), NOW())
                    """)
                    .setParameter("sellerId", sellerId)
                    .setParameter("amount", amount)
                    .setParameter("currency", currency)
                    .setParameter("orderId", razorpayOrderId)
                    .setParameter("receipt", receipt)
                    .setParameter("displayOrderNumber", displayOrderNumber)
                    .executeUpdate();
        }
    }

    @Transactional
    public void markPaid(
            Long sellerId,
            String razorpayOrderId,
            String paymentId,
            String signature,
            String invoiceNumber) {
        entityManager.createNativeQuery("""
                UPDATE seller_registration_payments
                SET razorpay_payment_id = :paymentId,
                    razorpay_signature = :signature,
                    invoice_number = :invoiceNumber,
                    status = 'paid',
                    paid_at = NOW(),
                    updated_at = NOW()
                WHERE seller_id = :sellerId
                  AND razorpay_order_id = :orderId
                """)
                .setParameter("sellerId", sellerId)
                .setParameter("orderId", razorpayOrderId)
                .setParameter("paymentId", paymentId)
                .setParameter("signature", signature)
                .setParameter("invoiceNumber", invoiceNumber)
                .executeUpdate();
    }

    public boolean isPaid(Long sellerId) {
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(1)
                FROM seller_registration_payments
                WHERE seller_id = :sellerId
                  AND status = 'paid'
                """)
                .setParameter("sellerId", sellerId)
                .getSingleResult();
        return count != null && count.longValue() > 0;
    }

    public static class PaymentRecord {
        private Long sellerId;
        private int amount;
        private String currency;
        private String orderId;
        private String paymentId;
        private String paymentSignature;
        private String displayOrderNumber;
        private String invoiceNumber;
        private String status;
        private String receipt;
        private LocalDateTime paidAt;
        private LocalDateTime createdAt;

        public Long getSellerId() { return sellerId; }
        public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
        public int getAmount() { return amount; }
        public void setAmount(int amount) { this.amount = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
        public String getPaymentSignature() { return paymentSignature; }
        public void setPaymentSignature(String paymentSignature) { this.paymentSignature = paymentSignature; }
        public String getDisplayOrderNumber() { return displayOrderNumber; }
        public void setDisplayOrderNumber(String displayOrderNumber) { this.displayOrderNumber = displayOrderNumber; }
        public String getInvoiceNumber() { return invoiceNumber; }
        public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getReceipt() { return receipt; }
        public void setReceipt(String receipt) { this.receipt = receipt; }
        public LocalDateTime getPaidAt() { return paidAt; }
        public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}

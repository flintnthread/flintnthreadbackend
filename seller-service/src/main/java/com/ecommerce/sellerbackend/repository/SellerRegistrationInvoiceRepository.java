package com.ecommerce.sellerbackend.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class SellerRegistrationInvoiceRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void ensureTable() {
        entityManager.createNativeQuery("""
                CREATE TABLE IF NOT EXISTS seller_registration_invoices (
                  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                  seller_id BIGINT NOT NULL,
                  invoice_number VARCHAR(32) NOT NULL,
                  display_order_number VARCHAR(32),
                  razorpay_payment_id VARCHAR(80),
                  amount INT NOT NULL,
                  currency VARCHAR(8) NOT NULL DEFAULT 'INR',
                  paid_at DATETIME NOT NULL,
                  invoice_pdf LONGBLOB NOT NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  INDEX idx_seller_registration_invoices_seller (seller_id)
                )
                """).executeUpdate();
    }

    @Transactional
    public Long saveInvoice(
            Long sellerId,
            String invoiceNumber,
            String displayOrderNumber,
            String razorpayPaymentId,
            int amount,
            String currency,
            LocalDateTime paidAt,
            byte[] invoicePdf) {
        ensureTable();
        entityManager.createNativeQuery("""
                INSERT INTO seller_registration_invoices
                (seller_id, invoice_number, display_order_number, razorpay_payment_id, amount, currency, paid_at, invoice_pdf, created_at)
                VALUES (:sellerId, :invoiceNumber, :displayOrderNumber, :paymentId, :amount, :currency, :paidAt, :invoicePdf, NOW())
                """)
                .setParameter("sellerId", sellerId)
                .setParameter("invoiceNumber", invoiceNumber)
                .setParameter("displayOrderNumber", displayOrderNumber)
                .setParameter("paymentId", razorpayPaymentId)
                .setParameter("amount", amount)
                .setParameter("currency", currency)
                .setParameter("paidAt", Timestamp.valueOf(paidAt))
                .setParameter("invoicePdf", invoicePdf)
                .executeUpdate();

        Number id = (Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult();
        return id != null ? id.longValue() : null;
    }

    @SuppressWarnings("unchecked")
    public List<InvoiceRecord> findBySellerId(Long sellerId) {
        ensureTable();
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT id, invoice_number, display_order_number, razorpay_payment_id, amount, currency, paid_at
                FROM seller_registration_invoices
                WHERE seller_id = :sellerId
                ORDER BY paid_at DESC
                """)
                .setParameter("sellerId", sellerId)
                .getResultList();

        List<InvoiceRecord> records = new ArrayList<>();
        for (Object[] row : rows) {
            InvoiceRecord record = new InvoiceRecord();
            record.setId(((Number) row[0]).longValue());
            record.setInvoiceNumber(row[1] != null ? row[1].toString() : null);
            record.setDisplayOrderNumber(row[2] != null ? row[2].toString() : null);
            record.setPaymentId(row[3] != null ? row[3].toString() : null);
            record.setAmount(((Number) row[4]).intValue());
            record.setCurrency(row[5] != null ? row[5].toString() : "INR");
            if (row[6] instanceof Timestamp ts) {
                record.setPaidAt(ts.toLocalDateTime());
            }
            records.add(record);
        }
        return records;
    }

    public InvoiceRecord findRecordById(Long sellerId, Long invoiceId) {
        ensureTable();
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT id, invoice_number, display_order_number, razorpay_payment_id, amount, currency, paid_at
                FROM seller_registration_invoices
                WHERE seller_id = :sellerId AND id = :invoiceId
                LIMIT 1
                """)
                .setParameter("sellerId", sellerId)
                .setParameter("invoiceId", invoiceId)
                .getResultList();
        if (rows.isEmpty()) {
            return null;
        }
        Object[] row = rows.get(0);
        InvoiceRecord record = new InvoiceRecord();
        record.setId(((Number) row[0]).longValue());
        record.setInvoiceNumber(row[1] != null ? row[1].toString() : null);
        record.setDisplayOrderNumber(row[2] != null ? row[2].toString() : null);
        record.setPaymentId(row[3] != null ? row[3].toString() : null);
        record.setAmount(((Number) row[4]).intValue());
        record.setCurrency(row[5] != null ? row[5].toString() : "INR");
        if (row[6] instanceof Timestamp ts) {
            record.setPaidAt(ts.toLocalDateTime());
        }
        return record;
    }

    @Transactional
    public void updateInvoicePdf(Long sellerId, Long invoiceId, int amountPaise, byte[] invoicePdf) {
        ensureTable();
        entityManager.createNativeQuery("""
                UPDATE seller_registration_invoices
                SET amount = :amount,
                    invoice_pdf = :invoicePdf
                WHERE seller_id = :sellerId AND id = :invoiceId
                """)
                .setParameter("sellerId", sellerId)
                .setParameter("invoiceId", invoiceId)
                .setParameter("amount", amountPaise)
                .setParameter("invoicePdf", invoicePdf)
                .executeUpdate();
    }

    public InvoicePdfRecord findPdfById(Long sellerId, Long invoiceId) {
        ensureTable();
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT invoice_number, invoice_pdf
                FROM seller_registration_invoices
                WHERE seller_id = :sellerId AND id = :invoiceId
                LIMIT 1
                """)
                .setParameter("sellerId", sellerId)
                .setParameter("invoiceId", invoiceId)
                .getResultList();
        if (rows.isEmpty()) {
            return null;
        }
        Object[] row = rows.get(0);
        InvoicePdfRecord record = new InvoicePdfRecord();
        record.setInvoiceNumber(row[0] != null ? row[0].toString() : "invoice.pdf");
        record.setPdfBytes(row[1] instanceof byte[] bytes ? bytes : null);
        return record;
    }

    public static class InvoiceRecord {
        private Long id;
        private String invoiceNumber;
        private String displayOrderNumber;
        private String paymentId;
        private int amount;
        private String currency;
        private LocalDateTime paidAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getInvoiceNumber() { return invoiceNumber; }
        public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
        public String getDisplayOrderNumber() { return displayOrderNumber; }
        public void setDisplayOrderNumber(String displayOrderNumber) { this.displayOrderNumber = displayOrderNumber; }
        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
        public int getAmount() { return amount; }
        public void setAmount(int amount) { this.amount = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public LocalDateTime getPaidAt() { return paidAt; }
        public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    }

    public static class InvoicePdfRecord {
        private String invoiceNumber;
        private byte[] pdfBytes;

        public String getInvoiceNumber() { return invoiceNumber; }
        public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
        public byte[] getPdfBytes() { return pdfBytes; }
        public void setPdfBytes(byte[] pdfBytes) { this.pdfBytes = pdfBytes; }
    }
}

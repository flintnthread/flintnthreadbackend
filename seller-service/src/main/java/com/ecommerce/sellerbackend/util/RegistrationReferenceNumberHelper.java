package com.ecommerce.sellerbackend.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class RegistrationReferenceNumberHelper {

    private static final DateTimeFormatter ORDER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter INVOICE_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private RegistrationReferenceNumberHelper() {
    }

    /** e.g. FNT202606206723 */
    public static String buildDisplayOrderNumber(Long sellerId, LocalDate orderDate) {
        return "FNT"
                + orderDate.format(ORDER_DATE)
                + String.format(Locale.ENGLISH, "%04d", sellerId);
    }

    /** e.g. INV-2026-000213 */
    public static String buildInvoiceNumber(Long sellerId, int paymentYear) {
        return "INV-"
                + paymentYear
                + "-"
                + String.format(Locale.ENGLISH, "%06d", sellerId);
    }

    public static String formatInvoiceDate(LocalDateTime paidAt) {
        LocalDateTime value = paidAt != null ? paidAt : LocalDateTime.now();
        return value.format(INVOICE_DATE);
    }
}

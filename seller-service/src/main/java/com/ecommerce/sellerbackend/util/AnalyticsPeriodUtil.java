package com.ecommerce.sellerbackend.util;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class AnalyticsPeriodUtil {

    private AnalyticsPeriodUtil() {
    }

    public static LocalDateTime[] resolveRange(String period) {
        LocalDate today = LocalDate.now();
        LocalDateTime from;
        LocalDateTime to = today.plusDays(1).atStartOfDay();
        if ("day".equalsIgnoreCase(period)) {
            from = today.atStartOfDay();
        } else if ("week".equalsIgnoreCase(period)) {
            from = today.minusDays(6).atStartOfDay();
        } else if ("year".equalsIgnoreCase(period)) {
            from = today.withDayOfYear(1).atStartOfDay();
        } else {
            from = today.withDayOfMonth(1).atStartOfDay();
        }
        return new LocalDateTime[] { from, to };
    }

    public static LocalDateTime[] resolveRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("Both from and to dates are required.");
        }
        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("End date must be on or after start date.");
        }
        return new LocalDateTime[] {
                fromDate.atStartOfDay(),
                toDate.plusDays(1).atStartOfDay()
        };
    }

    public static String mapDashboardPeriod(String period) {
        if (period == null) {
            return "week";
        }
        return switch (period.trim().toLowerCase()) {
            case "day" -> "day";
            case "month" -> "month";
            case "year" -> "year";
            default -> "week";
        };
    }
}

package com.ecommerce.adminbackend.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;

public final class SellerGraphPeriodUtil {

    private static final DateTimeFormatter INPUT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private SellerGraphPeriodUtil() {
    }

    public record PeriodBucket(String label, LocalDateTime periodStart, LocalDateTime periodEnd) {
    }

    public static LocalDate parseInputDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), INPUT_DATE);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    public static List<PeriodBucket> buildBuckets(String filterType, Integer year, String fromDate, String toDate) {
        LocalDate from = parseInputDate(fromDate);
        LocalDate to = parseInputDate(toDate);
        if (from != null && to != null && !to.isBefore(from)) {
            return buildCustomRangeBuckets(filterType, from, to);
        }

        String normalized = filterType == null ? "Monthly" : filterType.trim();
        int selectedYear = year != null ? year : LocalDate.now().getYear();

        return switch (normalized) {
            case "Weekly" -> buildWeeklyBuckets(selectedYear);
            case "Quarterly" -> buildQuarterlyBuckets(selectedYear);
            case "Overall" -> buildOverallBuckets();
            default -> buildMonthlyBuckets(selectedYear);
        };
    }

    private static List<PeriodBucket> buildMonthlyBuckets(int year) {
        List<PeriodBucket> buckets = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            YearMonth ym = YearMonth.of(year, month);
            LocalDateTime start = ym.atDay(1).atStartOfDay();
            LocalDateTime end = ym.atEndOfMonth().atTime(LocalTime.MAX);
            buckets.add(new PeriodBucket(Month.of(month).name().substring(0, 1)
                    + Month.of(month).name().substring(1, 3).toLowerCase(), start, end));
        }
        return buckets;
    }

    private static List<PeriodBucket> buildQuarterlyBuckets(int year) {
        List<PeriodBucket> buckets = new ArrayList<>();
        for (int quarter = 1; quarter <= 4; quarter++) {
            int startMonth = (quarter - 1) * 3 + 1;
            int endMonth = quarter * 3;
            LocalDateTime start = YearMonth.of(year, startMonth).atDay(1).atStartOfDay();
            LocalDateTime end = YearMonth.of(year, endMonth).atEndOfMonth().atTime(LocalTime.MAX);
            buckets.add(new PeriodBucket("Q" + quarter, start, end));
        }
        return buckets;
    }

    private static List<PeriodBucket> buildWeeklyBuckets(int year) {
        List<PeriodBucket> buckets = new ArrayList<>();
        LocalDate cursor = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);
        int week = 1;
        while (!cursor.isAfter(yearEnd)) {
            LocalDate weekEnd = cursor.plusDays(6);
            if (weekEnd.isAfter(yearEnd)) {
                weekEnd = yearEnd;
            }
            buckets.add(new PeriodBucket("W" + week, cursor.atStartOfDay(), weekEnd.atTime(LocalTime.MAX)));
            cursor = weekEnd.plusDays(1);
            week++;
        }
        return buckets;
    }

    private static List<PeriodBucket> buildOverallBuckets() {
        List<PeriodBucket> buckets = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = 11; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            String label = Month.of(ym.getMonthValue()).name().substring(0, 1)
                    + Month.of(ym.getMonthValue()).name().substring(1, 3).toLowerCase();
            LocalDateTime start = ym.atDay(1).atStartOfDay();
            LocalDateTime end = ym.atEndOfMonth().atTime(LocalTime.MAX);
            buckets.add(new PeriodBucket(label, start, end));
        }
        return buckets;
    }

    private static List<PeriodBucket> buildCustomRangeBuckets(String filterType, LocalDate from, LocalDate to) {
        String normalized = filterType == null ? "Monthly" : filterType.trim();
        return switch (normalized) {
            case "Weekly" -> buildWeeklyRangeBuckets(from, to);
            case "Quarterly" -> buildQuarterlyRangeBuckets(from, to);
            default -> buildMonthlyRangeBuckets(from, to);
        };
    }

    private static List<PeriodBucket> buildMonthlyRangeBuckets(LocalDate from, LocalDate to) {
        List<PeriodBucket> buckets = new ArrayList<>();
        YearMonth cursor = YearMonth.from(from);
        YearMonth end = YearMonth.from(to);
        while (!cursor.isAfter(end)) {
            String label = Month.of(cursor.getMonthValue()).name().substring(0, 1)
                    + Month.of(cursor.getMonthValue()).name().substring(1, 3).toLowerCase();
            LocalDateTime bucketStart = cursor.atDay(1).atStartOfDay();
            if (bucketStart.toLocalDate().isBefore(from)) {
                bucketStart = from.atStartOfDay();
            }
            LocalDateTime bucketEnd = cursor.atEndOfMonth().atTime(LocalTime.MAX);
            if (bucketEnd.toLocalDate().isAfter(to)) {
                bucketEnd = to.atTime(LocalTime.MAX);
            }
            buckets.add(new PeriodBucket(label, bucketStart, bucketEnd));
            cursor = cursor.plusMonths(1);
        }
        return buckets;
    }

    private static List<PeriodBucket> buildQuarterlyRangeBuckets(LocalDate from, LocalDate to) {
        List<PeriodBucket> buckets = new ArrayList<>();
        LocalDate cursor = from.with(IsoFields.DAY_OF_QUARTER, 1);
        while (!cursor.isAfter(to)) {
            int quarter = cursor.get(IsoFields.QUARTER_OF_YEAR);
            LocalDate quarterStart = cursor;
            LocalDate quarterEnd = cursor.plusMonths(3 - ((cursor.getMonthValue() - 1) % 3)).withDayOfMonth(1)
                    .plusMonths(1).minusDays(1);
            if (quarterEnd.isAfter(to)) {
                quarterEnd = to;
            }
            LocalDateTime bucketStart = quarterStart.isBefore(from) ? from.atStartOfDay() : quarterStart.atStartOfDay();
            buckets.add(new PeriodBucket("Q" + quarter, bucketStart, quarterEnd.atTime(LocalTime.MAX)));
            cursor = quarterEnd.plusDays(1);
        }
        return buckets;
    }

    private static List<PeriodBucket> buildWeeklyRangeBuckets(LocalDate from, LocalDate to) {
        List<PeriodBucket> buckets = new ArrayList<>();
        LocalDate cursor = from;
        int week = 1;
        while (!cursor.isAfter(to)) {
            LocalDate weekEnd = cursor.plusDays(6);
            if (weekEnd.isAfter(to)) {
                weekEnd = to;
            }
            buckets.add(new PeriodBucket("W" + week, cursor.atStartOfDay(), weekEnd.atTime(LocalTime.MAX)));
            cursor = weekEnd.plusDays(1);
            week++;
        }
        return buckets;
    }

    public static LocalDateTime resolveSummaryRangeStart(String filterType, Integer year, String fromDate, String toDate) {
        List<PeriodBucket> buckets = buildBuckets(filterType, year, fromDate, toDate);
        if (buckets.isEmpty()) {
            return null;
        }
        return buckets.get(0).periodStart();
    }

    public static LocalDateTime resolveSummaryRangeEnd(String filterType, Integer year, String fromDate, String toDate) {
        List<PeriodBucket> buckets = buildBuckets(filterType, year, fromDate, toDate);
        if (buckets.isEmpty()) {
            return null;
        }
        return buckets.get(buckets.size() - 1).periodEnd();
    }

    public static boolean hasBoundedRange(String filterType, Integer year, String fromDate, String toDate) {
        if (parseInputDate(fromDate) != null && parseInputDate(toDate) != null) {
            return true;
        }
        if (year != null) {
            return true;
        }
        return "Overall".equalsIgnoreCase(filterType != null ? filterType.trim() : "");
    }
}

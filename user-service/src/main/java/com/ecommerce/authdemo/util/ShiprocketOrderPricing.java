package com.ecommerce.authdemo.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds Shiprocket money fields so the amount equals the FNT checkout / order total exactly.
 * Shiprocket {@code sub_total} = order placing total; {@code shipping_charges} = 0 to avoid double shipping.
 */
public final class ShiprocketOrderPricing {

    private ShiprocketOrderPricing() {
    }

    public record LineInput(String name, String sku, String hsn, int units, BigDecimal unitPrice, BigDecimal lineTotal) {
    }

    public record PricedPayload(
            List<Map<String, Object>> orderItems,
            BigDecimal subTotal,
            BigDecimal shippingCharges,
            BigDecimal totalDiscount,
            BigDecimal orderTotal
    ) {
    }

    public static PricedPayload build(
            List<LineInput> lines,
            BigDecimal orderTotalAmount,
            BigDecimal shippingAmount,
            BigDecimal discountAmount,
            BigDecimal referralDiscountAmount
    ) {
        BigDecimal orderTotal = money(orderTotalAmount);

        List<Map<String, Object>> orderItems = new ArrayList<>();
        BigDecimal itemsGross = BigDecimal.ZERO;

        if (lines != null) {
            for (LineInput line : lines) {
                if (line == null) {
                    continue;
                }
                int units = Math.max(1, line.units());
                BigDecimal unit = resolveUnitPrice(line.unitPrice(), line.lineTotal(), units);
                itemsGross = itemsGross.add(unit.multiply(BigDecimal.valueOf(units)));

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", line.name() != null && !line.name().isBlank() ? line.name() : "Product");
                row.put("sku", line.sku() != null && !line.sku().isBlank() ? line.sku() : "SKU");
                row.put("units", units);
                row.put("selling_price", unit.doubleValue());
                row.put("hsn", line.hsn() != null && !line.hsn().isBlank() ? line.hsn() : "0000");
                orderItems.add(row);
            }
        }

        if (orderTotal.compareTo(BigDecimal.ZERO) <= 0 && itemsGross.compareTo(BigDecimal.ZERO) > 0) {
            orderTotal = itemsGross;
        }

        BigDecimal subTotal = orderTotal;

        BigDecimal totalDiscount = itemsGross.subtract(subTotal);
        if (totalDiscount.compareTo(BigDecimal.ZERO) < 0) {
            totalDiscount = BigDecimal.ZERO;
            if (!orderItems.isEmpty()) {
                BigDecimal gap = subTotal.subtract(itemsGross);
                Map<String, Object> last = orderItems.get(orderItems.size() - 1);
                int units = ((Number) last.get("units")).intValue();
                double currentUnit = ((Number) last.get("selling_price")).doubleValue();
                BigDecimal perUnitBump = gap.divide(BigDecimal.valueOf(units), 2, RoundingMode.HALF_UP);
                last.put("selling_price", money(BigDecimal.valueOf(currentUnit).add(perUnitBump)).doubleValue());
                itemsGross = itemsGross.add(perUnitBump.multiply(BigDecimal.valueOf(units)));
                BigDecimal leftover = itemsGross.subtract(subTotal);
                if (leftover.compareTo(BigDecimal.ZERO) > 0) {
                    totalDiscount = leftover;
                }
            }
        }

        BigDecimal storedDiscount = money(discountAmount).add(money(referralDiscountAmount));
        if (storedDiscount.compareTo(BigDecimal.ZERO) > 0
                && totalDiscount.compareTo(BigDecimal.ZERO) == 0
                && itemsGross.subtract(storedDiscount).compareTo(subTotal) == 0) {
            totalDiscount = storedDiscount;
        }

        if (subTotal.compareTo(BigDecimal.ZERO) > 0 && itemsGross.compareTo(BigDecimal.ZERO) <= 0 && !orderItems.isEmpty()) {
            distributeAmountAcrossItems(orderItems, subTotal);
            itemsGross = subTotal;
            totalDiscount = BigDecimal.ZERO;
        }

        return new PricedPayload(
                orderItems,
                money(subTotal),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                money(totalDiscount),
                money(orderTotal)
        );
    }

    private static void distributeAmountAcrossItems(List<Map<String, Object>> orderItems, BigDecimal amount) {
        if (orderItems.isEmpty()) {
            return;
        }
        int totalUnits = orderItems.stream()
                .mapToInt(row -> ((Number) row.get("units")).intValue())
                .sum();
        if (totalUnits <= 0) {
            orderItems.get(0).put("selling_price", money(amount).doubleValue());
            return;
        }
        BigDecimal remaining = money(amount);
        for (int i = 0; i < orderItems.size(); i++) {
            Map<String, Object> row = orderItems.get(i);
            int units = ((Number) row.get("units")).intValue();
            BigDecimal lineAmount;
            if (i == orderItems.size() - 1) {
                lineAmount = remaining;
            } else {
                lineAmount = amount.multiply(BigDecimal.valueOf(units))
                        .divide(BigDecimal.valueOf(totalUnits), 2, RoundingMode.HALF_UP);
                remaining = remaining.subtract(lineAmount);
            }
            row.put("selling_price", money(lineAmount.divide(BigDecimal.valueOf(units), 2, RoundingMode.HALF_UP)).doubleValue());
        }
    }

    static BigDecimal resolveUnitPrice(BigDecimal unitPrice, BigDecimal lineTotal, int units) {
        BigDecimal unit = money(unitPrice);
        if (unit.compareTo(BigDecimal.ZERO) > 0) {
            return unit;
        }
        BigDecimal total = money(lineTotal);
        if (total.compareTo(BigDecimal.ZERO) > 0 && units > 0) {
            return total.divide(BigDecimal.valueOf(units), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal toMoney(Double value) {
        if (value == null || !Double.isFinite(value)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}

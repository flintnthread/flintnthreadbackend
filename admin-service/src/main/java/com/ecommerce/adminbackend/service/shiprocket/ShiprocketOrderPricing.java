package com.ecommerce.adminbackend.service.shiprocket;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds Shiprocket money fields so the amount equals the FNT checkout / order total exactly.
 * <p>
 * Shiprocket uses {@code sub_total} as the order value (and COD collect amount).
 * FNT {@code totalAmount} already includes shipping, discounts, platform fee and wallet —
 * so we send that exact value as {@code sub_total} and set {@code shipping_charges} to 0
 * to avoid Shiprocket adding shipping again and changing the price.
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
        // Exact amount the customer paid / must pay at order placing.
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
                row.put("selling_price", unit);
                row.put("hsn", line.hsn() != null && !line.hsn().isBlank() ? line.hsn() : "0000");
                orderItems.add(row);
            }
        }

        // sub_total MUST equal FNT order total — same price as order placing.
        BigDecimal subTotal = orderTotal;

        // Reconcile line items: itemsGross - total_discount == sub_total
        BigDecimal totalDiscount = itemsGross.subtract(subTotal);
        if (totalDiscount.compareTo(BigDecimal.ZERO) < 0) {
            // Order total higher than item gross (shipping / platform fee baked into totalAmount).
            // Raise last line unit price so Shiprocket item math equals the order total.
            totalDiscount = BigDecimal.ZERO;
            if (!orderItems.isEmpty()) {
                BigDecimal gap = subTotal.subtract(itemsGross);
                Map<String, Object> last = orderItems.get(orderItems.size() - 1);
                int units = ((Number) last.get("units")).intValue();
                BigDecimal currentUnit = (BigDecimal) last.get("selling_price");
                BigDecimal perUnitBump = gap.divide(BigDecimal.valueOf(units), 2, RoundingMode.HALF_UP);
                last.put("selling_price", money(currentUnit.add(perUnitBump)));
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

        // shipping_charges = 0: shipping is already inside orderTotal / sub_total.
        // Passing shipping again would change the Shiprocket displayed/COD price.
        return new PricedPayload(
                orderItems,
                money(subTotal),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                money(totalDiscount),
                money(orderTotal)
        );
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
}

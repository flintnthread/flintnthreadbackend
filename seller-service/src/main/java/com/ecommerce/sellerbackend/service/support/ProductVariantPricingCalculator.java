package com.ecommerce.sellerbackend.service.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ProductVariantPricingCalculator {

    public static final BigDecimal DEFAULT_GST = new BigDecimal("5.00");
    public static final BigDecimal COMMISSION_PERCENT = new BigDecimal("15.00");
    public static final BigDecimal DEFAULT_INTRA_CITY = new BigDecimal("175.00");
    public static final BigDecimal DEFAULT_METRO_METRO = new BigDecimal("205.00");

    private ProductVariantPricingCalculator() {}

    public record VariantPricing(
            BigDecimal discountPercentage,
            BigDecimal discountAmount,
            BigDecimal taxAmount,
            BigDecimal finalPrice,
            BigDecimal mrpInclGst,
            BigDecimal commissionAmount,
            BigDecimal totalIntraCity,
            BigDecimal totalMetroMetro) {}

    public static VariantPricing calculate(
            BigDecimal mrpExcl,
            BigDecimal sellingExcl,
            BigDecimal discountOverride,
            BigDecimal gstPercent) {
        return calculate(mrpExcl, sellingExcl, discountOverride, gstPercent, DEFAULT_INTRA_CITY, DEFAULT_METRO_METRO);
    }

    public static VariantPricing calculate(
            BigDecimal mrpExcl,
            BigDecimal sellingExcl,
            BigDecimal discountOverride,
            BigDecimal gstPercent,
            BigDecimal intraCityCharge,
            BigDecimal metroMetroCharge) {
        return calculate(
                mrpExcl,
                sellingExcl,
                discountOverride,
                gstPercent,
                intraCityCharge,
                metroMetroCharge,
                COMMISSION_PERCENT);
    }

    public static VariantPricing calculate(
            BigDecimal mrpExcl,
            BigDecimal sellingExcl,
            BigDecimal discountOverride,
            BigDecimal gstPercent,
            BigDecimal intraCityCharge,
            BigDecimal metroMetroCharge,
            BigDecimal commissionPercent) {
        BigDecimal gst = gstPercent != null ? gstPercent : DEFAULT_GST;
        BigDecimal intra = intraCityCharge != null ? intraCityCharge : DEFAULT_INTRA_CITY;
        BigDecimal metro = metroMetroCharge != null ? metroMetroCharge : DEFAULT_METRO_METRO;
        BigDecimal commission = commissionPercent != null ? commissionPercent : COMMISSION_PERCENT;
        BigDecimal discountAmount = mrpExcl.subtract(sellingExcl).max(BigDecimal.ZERO);
        BigDecimal discountPercentage = discountOverride;
        if (discountPercentage == null && mrpExcl.compareTo(BigDecimal.ZERO) > 0) {
            discountPercentage = discountAmount
                    .multiply(BigDecimal.valueOf(100))
                    .divide(mrpExcl, 2, RoundingMode.HALF_UP);
        }
        if (discountPercentage == null) {
            discountPercentage = BigDecimal.ZERO;
        }

        BigDecimal taxAmount = sellingExcl
                .multiply(gst)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal finalPrice = sellingExcl.add(taxAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gstFactor = BigDecimal.ONE.add(
                gst.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        BigDecimal mrpInclGst = mrpExcl.multiply(gstFactor).setScale(2, RoundingMode.HALF_UP);
        BigDecimal commissionAmount = finalPrice
                .multiply(commission)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal totalIntraCity = finalPrice.add(intra).add(commissionAmount)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalMetroMetro = finalPrice.add(metro).add(commissionAmount)
                .setScale(2, RoundingMode.HALF_UP);

        return new VariantPricing(
                discountPercentage,
                discountAmount,
                taxAmount,
                finalPrice,
                mrpInclGst,
                commissionAmount,
                totalIntraCity,
                totalMetroMetro);
    }
}

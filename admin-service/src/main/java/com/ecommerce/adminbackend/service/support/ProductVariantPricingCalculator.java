package com.ecommerce.adminbackend.service.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ProductVariantPricingCalculator {

    public static final BigDecimal DEFAULT_GST = new BigDecimal("5.00");
    public static final BigDecimal COMMISSION_PERCENT = BigDecimal.ZERO;
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
            BigDecimal gstPercent,
            BigDecimal intraCityCharge,
            BigDecimal metroMetroCharge) {
        BigDecimal gst = gstPercent != null ? gstPercent : DEFAULT_GST;
        BigDecimal intra = intraCityCharge != null ? intraCityCharge : DEFAULT_INTRA_CITY;
        BigDecimal metro = metroMetroCharge != null ? metroMetroCharge : DEFAULT_METRO_METRO;
        BigDecimal mrp = mrpExcl != null ? mrpExcl : BigDecimal.ZERO;
        BigDecimal selling = sellingExcl != null ? sellingExcl : BigDecimal.ZERO;

        BigDecimal discountAmount = mrp.subtract(selling).max(BigDecimal.ZERO);
        BigDecimal discountPercentage = discountOverride;
        if (discountPercentage == null && mrp.compareTo(BigDecimal.ZERO) > 0) {
            discountPercentage = discountAmount
                    .multiply(BigDecimal.valueOf(100))
                    .divide(mrp, 2, RoundingMode.HALF_UP);
        }
        if (discountPercentage == null) {
            discountPercentage = BigDecimal.ZERO;
        }

        BigDecimal taxAmount = selling
                .multiply(gst)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal finalPrice = selling.add(taxAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gstFactor = BigDecimal.ONE.add(
                gst.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        BigDecimal mrpInclGst = mrp.multiply(gstFactor).setScale(2, RoundingMode.HALF_UP);
        BigDecimal commissionAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
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

package com.ecommerce.adminbackend.service.support;

import com.ecommerce.adminbackend.entity.AdminSetting;
import com.ecommerce.adminbackend.entity.ProductVariant;
import com.ecommerce.adminbackend.entity.Seller;
import com.ecommerce.adminbackend.entity.SellerCategory;
import com.ecommerce.adminbackend.repository.AdminSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
public class ProductVariantCommissionSupport {

    private static final String KEY_B2C = "commission_b2c";
    private static final String KEY_B2B = "commission_b2b";
    private static final BigDecimal DEFAULT_B2C = new BigDecimal("15");
    private static final BigDecimal DEFAULT_B2B = new BigDecimal("7");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final AdminSettingRepository adminSettingRepository;

    public record EnrichedPricing(
            BigDecimal sellingPriceWithGst,
            BigDecimal commissionPercentage,
            BigDecimal commissionAmount,
            BigDecimal priceWithCommission,
            BigDecimal highestDeliveryCharge,
            BigDecimal displayPrice,
            BigDecimal totalPriceIntraCity,
            BigDecimal totalPriceMetroMetro) {}

    public BigDecimal resolveCommissionPercent(Seller seller) {
        if (seller != null
                && seller.getSellerCategory() == SellerCategory.b2b) {
            return readCommissionPercent(KEY_B2B, DEFAULT_B2B);
        }
        return readCommissionPercent(KEY_B2C, DEFAULT_B2C);
    }

    public EnrichedPricing enrich(ProductVariant variant, BigDecimal commissionPercent, BigDecimal defaultGst) {
        return enrich(variant, commissionPercent, defaultGst, false);
    }

    /**
     * @param forcePlatformRate when true, always use {@code commissionPercent} (admin rate save / approval).
     *                          when false, keep existing variant commission if already stored.
     */
    public EnrichedPricing enrich(
            ProductVariant variant,
            BigDecimal commissionPercent,
            BigDecimal defaultGst,
            boolean forcePlatformRate) {
        BigDecimal sellingWithGst = resolveSellingPriceWithGst(variant, defaultGst);
        BigDecimal intra = nonNegative(variant.getIntraCityDeliveryCharge());
        BigDecimal metro = nonNegative(variant.getMetroMetroDeliveryCharge());

        BigDecimal commissionPct;
        BigDecimal commissionAmt;
        if (!forcePlatformRate
                && variant.getCommissionPercentage() != null
                && variant.getCommissionPercentage().compareTo(BigDecimal.ZERO) > 0
                && variant.getCommissionAmount() != null
                && variant.getCommissionAmount().compareTo(BigDecimal.ZERO) > 0) {
            commissionPct = variant.getCommissionPercentage();
            commissionAmt = variant.getCommissionAmount().setScale(2, RoundingMode.HALF_UP);
        } else {
            commissionPct = commissionPercent != null ? commissionPercent : BigDecimal.ZERO;
            commissionAmt = sellingWithGst
                    .multiply(commissionPct)
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        }

        BigDecimal priceBeforeDelivery = sellingWithGst.add(commissionAmt).setScale(2, RoundingMode.HALF_UP);
        BigDecimal highestDelivery = intra.max(metro);
        BigDecimal displayPrice = priceBeforeDelivery.add(highestDelivery).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalIntra = priceBeforeDelivery.add(intra).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalMetro = priceBeforeDelivery.add(metro).setScale(2, RoundingMode.HALF_UP);

        return new EnrichedPricing(
                sellingWithGst,
                commissionPct,
                commissionAmt,
                priceBeforeDelivery,
                highestDelivery,
                displayPrice,
                totalIntra,
                totalMetro);
    }

    public void applyCommission(ProductVariant variant, BigDecimal commissionPercent, BigDecimal defaultGst) {
        applyCommission(variant, commissionPercent, defaultGst, false);
    }

    public void applyCommission(
            ProductVariant variant,
            BigDecimal commissionPercent,
            BigDecimal defaultGst,
            boolean forcePlatformRate) {
        EnrichedPricing pricing = enrich(variant, commissionPercent, defaultGst, forcePlatformRate);
        variant.setCommissionPercentage(pricing.commissionPercentage());
        variant.setCommissionAmount(pricing.commissionAmount());
        variant.setTotalPriceIntraCity(pricing.totalPriceIntraCity());
        variant.setTotalPriceMetroMetro(pricing.totalPriceMetroMetro());
    }

    private BigDecimal resolveSellingPriceWithGst(ProductVariant variant, BigDecimal defaultGst) {
        if (variant.getFinalPrice() != null && variant.getFinalPrice().compareTo(BigDecimal.ZERO) > 0) {
            return variant.getFinalPrice().setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal sellingExcl = firstPositive(variant.getSellingPrice(), variant.getBasePrice());
        if (sellingExcl == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal gst = variant.getTaxPercentage() != null && variant.getTaxPercentage().compareTo(BigDecimal.ZERO) > 0
                ? variant.getTaxPercentage()
                : (defaultGst != null ? defaultGst : new BigDecimal("5"));
        BigDecimal taxAmount = variant.getTaxAmount() != null && variant.getTaxAmount().compareTo(BigDecimal.ZERO) > 0
                ? variant.getTaxAmount()
                : sellingExcl.multiply(gst).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        return sellingExcl.add(taxAmount).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal readCommissionPercent(String key, BigDecimal defaultValue) {
        return adminSettingRepository.findBySettingKey(key)
                .map(AdminSetting::getSettingValue)
                .filter(value -> value != null && !value.isBlank())
                .map(value -> new BigDecimal(value.trim()))
                .orElse(defaultValue);
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal firstPositive(BigDecimal... candidates) {
        for (BigDecimal candidate : candidates) {
            if (candidate != null && candidate.compareTo(BigDecimal.ZERO) > 0) {
                return candidate;
            }
        }
        return null;
    }
}

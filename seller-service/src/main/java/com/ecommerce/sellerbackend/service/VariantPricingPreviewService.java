package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.VariantPricingPreviewRequest;
import com.ecommerce.sellerbackend.dto.VariantPricingPreviewResponse;
import com.ecommerce.sellerbackend.entity.Subcategory;
import com.ecommerce.sellerbackend.repository.SubcategoryRepository;
import com.ecommerce.sellerbackend.service.support.ProductVariantPricingCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class VariantPricingPreviewService {

    private final DeliverySlabLookupService deliverySlabLookupService;
    private final SubcategoryRepository subcategoryRepository;
    private final AdminSettingsLookupService adminSettingsLookupService;

    public VariantPricingPreviewResponse preview(Long sellerId, VariantPricingPreviewRequest request) {
        BigDecimal mrpExcl = request.getMrpExcl() != null ? request.getMrpExcl() : BigDecimal.ZERO;
        BigDecimal sellingExcl = request.getSellingExcl() != null ? request.getSellingExcl() : BigDecimal.ZERO;
        BigDecimal gstPercent = request.getGstPercent() != null
                ? request.getGstPercent()
                : resolveGstPercent(request.getCategorySubId(), request.getSubcategoryId());
        BigDecimal commissionPercent = adminSettingsLookupService.getSellerCommissionPercent(sellerId);

        var slab = deliverySlabLookupService.resolveForWeight(request.getWeightKg());
        var pricing = ProductVariantPricingCalculator.calculate(
                mrpExcl,
                sellingExcl,
                request.getDiscountOverride(),
                gstPercent,
                slab.getIntraCityCharge(),
                slab.getMetroMetroCharge(),
                commissionPercent);

        return VariantPricingPreviewResponse.builder()
                .mrpExcl(mrpExcl)
                .sellingExcl(sellingExcl)
                .gstPercent(gstPercent)
                .discountPercentage(pricing.discountPercentage())
                .discountAmount(pricing.discountAmount())
                .taxAmount(pricing.taxAmount())
                .finalPrice(pricing.finalPrice())
                .mrpInclGst(pricing.mrpInclGst())
                .commissionPercent(commissionPercent)
                .commissionAmount(pricing.commissionAmount())
                .intraCityCharge(slab.getIntraCityCharge())
                .metroMetroCharge(slab.getMetroMetroCharge())
                .totalIntraCity(pricing.totalIntraCity())
                .totalMetroMetro(pricing.totalMetroMetro())
                .weightSlabLabel(slab.getLabel())
                .deliveryCustom(Boolean.TRUE.equals(slab.getCustom()))
                .build();
    }

    private BigDecimal resolveGstPercent(Integer categorySubId, Integer subcategoryId) {
        Integer subId = categorySubId != null ? categorySubId : subcategoryId;
        if (subId == null) {
            return ProductVariantPricingCalculator.DEFAULT_GST;
        }
        return subcategoryRepository.findById(subId)
                .map(Subcategory::getGstPercentage)
                .filter(gst -> gst != null)
                .orElse(ProductVariantPricingCalculator.DEFAULT_GST);
    }
}

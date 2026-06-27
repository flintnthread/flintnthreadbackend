package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class ProductFormCatalogResponse {
    private List<CatalogCategoryResponse> categories;
    private List<ColorResponse> colors;
    private List<SizeResponse> sizes;
    private List<DeliveryWeightSlabResponse> deliverySlabs;
    /** Min selling price across seller products (for filter slider). */
    private BigDecimal priceMin;
    /** Max selling price across seller products (for filter slider). */
    private BigDecimal priceMax;
    /** B2C commission % from admin_settings (commission_b2c). */
    private BigDecimal commissionPercent;
}

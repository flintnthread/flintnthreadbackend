package com.ecommerce.sellerbackend.controller;

import com.ecommerce.sellerbackend.dto.DeliveryWeightSlabResponse;
import com.ecommerce.sellerbackend.dto.ProductFormCatalogResponse;
import com.ecommerce.sellerbackend.dto.VariantPricingPreviewRequest;
import com.ecommerce.sellerbackend.dto.VariantPricingPreviewResponse;
import com.ecommerce.sellerbackend.service.DeliverySlabLookupService;
import com.ecommerce.sellerbackend.service.ProductCatalogService;
import com.ecommerce.sellerbackend.service.VariantPricingPreviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/seller/catalog")
@RequiredArgsConstructor
public class CatalogController {

    public static final String SELLER_ID_HEADER = "X-Seller-Id";

    private final ProductCatalogService productCatalogService;
    private final DeliverySlabLookupService deliverySlabLookupService;
    private final VariantPricingPreviewService variantPricingPreviewService;

    @GetMapping("/product-form")
    public ProductFormCatalogResponse productForm(@RequestHeader(SELLER_ID_HEADER) Long sellerId) {
        return productCatalogService.getProductFormCatalog(requireSellerId(sellerId));
    }

    @GetMapping("/delivery-charges")
    public DeliveryWeightSlabResponse deliveryChargesForWeight(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @RequestParam("weightKg") BigDecimal weightKg) {
        requireSellerId(sellerId);
        return deliverySlabLookupService.resolveForWeight(weightKg);
    }

    @PostMapping("/variant-pricing-preview")
    public VariantPricingPreviewResponse variantPricingPreview(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @RequestBody VariantPricingPreviewRequest request) {
        requireSellerId(sellerId);
        return variantPricingPreviewService.preview(request);
    }

    private Long requireSellerId(Long sellerId) {
        if (sellerId == null || sellerId <= 0) {
            throw new IllegalArgumentException("Valid seller id is required.");
        }
        return sellerId;
    }
}

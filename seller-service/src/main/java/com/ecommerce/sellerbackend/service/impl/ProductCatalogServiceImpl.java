package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.dto.CatalogCategoryResponse;
import com.ecommerce.sellerbackend.dto.ColorResponse;
import com.ecommerce.sellerbackend.dto.DeliveryWeightSlabResponse;
import com.ecommerce.sellerbackend.dto.ProductFormCatalogResponse;
import com.ecommerce.sellerbackend.dto.SizeResponse;
import com.ecommerce.sellerbackend.repository.ColorRepository;
import com.ecommerce.sellerbackend.repository.ProductVariantRepository;
import com.ecommerce.sellerbackend.repository.SizeRepository;
import com.ecommerce.sellerbackend.service.AdminSettingsLookupService;
import com.ecommerce.sellerbackend.service.DeliverySlabLookupService;
import com.ecommerce.sellerbackend.service.ProductCatalogService;
import com.ecommerce.sellerbackend.service.CatalogHierarchyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductCatalogServiceImpl implements ProductCatalogService {

    private final CatalogHierarchyService catalogHierarchyService;
    private final ColorRepository colorRepository;
    private final SizeRepository sizeRepository;
    private final DeliverySlabLookupService deliverySlabLookupService;
    private final ProductVariantRepository productVariantRepository;
    private final AdminSettingsLookupService adminSettingsLookupService;

    private static final BigDecimal DEFAULT_PRICE_MIN = BigDecimal.ZERO;
    private static final BigDecimal DEFAULT_PRICE_MAX = new BigDecimal("5000");

    @Override
    @Transactional(readOnly = true)
    public ProductFormCatalogResponse getProductFormCatalog(Long sellerId) {
        List<CatalogCategoryResponse> categoryResponses = catalogHierarchyService.loadCategoryTree();

        List<ColorResponse> colors = colorRepository.findVisibleForSeller(sellerId).stream()
                .filter(c -> Boolean.TRUE.equals(c.getStatus()))
                .map(c -> ColorResponse.from(c, sellerId))
                .toList();

        List<SizeResponse> sizes = sizeRepository.findVisibleForSeller(sellerId).stream()
                .filter(s -> Boolean.TRUE.equals(s.getStatus()))
                .map(s -> SizeResponse.from(s, sellerId))
                .toList();

        BigDecimal priceMin = productVariantRepository.findMinPriceForSeller(sellerId)
                .map(v -> v.setScale(0, RoundingMode.FLOOR))
                .orElse(DEFAULT_PRICE_MIN);
        BigDecimal priceMax = productVariantRepository.findMaxPriceForSeller(sellerId)
                .map(v -> {
                    BigDecimal rounded = v.setScale(0, RoundingMode.CEILING);
                    BigDecimal step = new BigDecimal("100");
                    BigDecimal mod = rounded.remainder(step);
                    return mod.signum() == 0 ? rounded : rounded.add(step.subtract(mod));
                })
                .orElse(DEFAULT_PRICE_MAX);
        if (priceMax.compareTo(priceMin) < 0) {
            priceMax = DEFAULT_PRICE_MAX;
            priceMin = DEFAULT_PRICE_MIN;
        }

        BigDecimal commissionPercent = adminSettingsLookupService.getSellerCommissionPercent(sellerId);

        return ProductFormCatalogResponse.builder()
                .categories(categoryResponses)
                .colors(colors)
                .sizes(sizes)
                .deliverySlabs(deliverySlabLookupService.listActiveSlabs())
                .priceMin(priceMin)
                .priceMax(priceMax.max(DEFAULT_PRICE_MAX))
                .commissionPercent(commissionPercent)
                .build();
    }
}

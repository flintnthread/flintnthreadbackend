package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.dto.CreateProductImageRequest;
import com.ecommerce.sellerbackend.dto.CreateProductResponse;
import com.ecommerce.sellerbackend.dto.CreateProductVariantRequest;
import com.ecommerce.sellerbackend.dto.VariantMutationRequest;
import com.ecommerce.sellerbackend.entity.Product;
import com.ecommerce.sellerbackend.entity.ProductImage;
import com.ecommerce.sellerbackend.entity.ProductVariant;
import com.ecommerce.sellerbackend.exception.ResourceNotFoundException;
import com.ecommerce.sellerbackend.repository.ProductImageRepository;
import com.ecommerce.sellerbackend.repository.ProductRepository;
import com.ecommerce.sellerbackend.repository.ProductVariantRepository;
import com.ecommerce.sellerbackend.service.ProductMediaStorageService;
import com.ecommerce.sellerbackend.service.support.ProductCatalogResolver;
import com.ecommerce.sellerbackend.service.support.ProductVariantPricingCalculator;
import com.ecommerce.sellerbackend.service.support.ProductVariantPricingCalculator.VariantPricing;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductVariantMutationService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductCatalogResolver catalogResolver;
    private final ProductMediaStorageService productMediaStorageService;

    @Transactional
    public CreateProductResponse.CreatedVariantRef createVariant(
            Long sellerId,
            Long productId,
            VariantMutationRequest request) {
        Product product = requireProduct(sellerId, productId);
        return saveVariant(product, null, request);
    }

    @Transactional
    public CreateProductResponse.CreatedVariantRef updateVariant(
            Long sellerId,
            Long productId,
            Long variantId,
            VariantMutationRequest request) {
        Product product = requireProduct(sellerId, productId);
        ProductVariant existing = productVariantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found."));
        return saveVariant(product, existing, request);
    }

    @Transactional
    public void deleteVariant(Long sellerId, Long productId, Long variantId) {
        requireProduct(sellerId, productId);
        ProductVariant variant = productVariantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found."));
        productImageRepository.findByProductId(productId).stream()
                .filter(img -> variantId.equals(img.getVariantId()))
                .forEach(img -> productImageRepository.deleteById(img.getId()));
        productVariantRepository.delete(variant);
    }

    private CreateProductResponse.CreatedVariantRef saveVariant(
            Product product,
            ProductVariant variant,
            VariantMutationRequest request) {
        LocalDateTime now = LocalDateTime.now();
        CreateProductVariantRequest catalogReq = new CreateProductVariantRequest();
        catalogReq.setColorId(request.getColorId());
        catalogReq.setColor(request.getColor());
        catalogReq.setSizeId(request.getSizeId());
        catalogReq.setSize(request.getSize());

        BigDecimal gstPercent = product.getGstPercentage() != null
                ? product.getGstPercentage()
                : ProductVariantPricingCalculator.DEFAULT_GST;

        VariantPricing pricing = ProductVariantPricingCalculator.calculate(
                request.getMrp(),
                request.getSellingPrice(),
                request.getDiscount(),
                gstPercent);

        if (variant == null) {
            variant = new ProductVariant();
            variant.setProductId(product.getId());
            variant.setCreatedAt(now);
        }

        variant.setColor(catalogResolver.resolveColorId(product.getSellerId(), catalogReq));
        variant.setSize(catalogResolver.resolveSizeId(product.getSellerId(), catalogReq));
        variant.setSku(request.getSku());
        variant.setStock(request.getStock());
        variant.setBasePrice(request.getSellingPrice());
        variant.setMrpExclGst(request.getMrp());
        variant.setMrpPrice(pricing.finalPrice());
        variant.setDiscountPercentage(pricing.discountPercentage());
        variant.setDiscountAmount(pricing.discountAmount());
        variant.setSellingPrice(request.getSellingPrice());
        variant.setTaxPercentage(gstPercent);
        variant.setTaxAmount(pricing.taxAmount());
        variant.setFinalPrice(pricing.finalPrice());
        variant.setMrpInclGst(pricing.mrpInclGst());
        variant.setIntraCityDeliveryCharge(ProductVariantPricingCalculator.DEFAULT_INTRA_CITY);
        variant.setMetroMetroDeliveryCharge(ProductVariantPricingCalculator.DEFAULT_METRO_METRO);
        variant.setCommissionPercentage(ProductVariantPricingCalculator.COMMISSION_PERCENT);
        variant.setCommissionAmount(pricing.commissionAmount());
        variant.setTotalPriceIntraCity(pricing.totalIntraCity());
        variant.setTotalPriceMetroMetro(pricing.totalMetroMetro());
        variant.setVideoPath(request.getVideoUrl());
        variant.setWeight(product.getProductWeight());
        variant.setUpdatedAt(now);

        variant = productVariantRepository.save(variant);
        final Long savedVariantId = variant.getId();

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            productImageRepository.findByProductId(product.getId()).stream()
                    .filter(img -> savedVariantId.equals(img.getVariantId()))
                    .forEach(img -> productImageRepository.deleteById(img.getId()));
            int sort = 0;
            for (CreateProductImageRequest imageReq : request.getImages()) {
                String path = productMediaStorageService.storeProductImage(imageReq.getSource());
                ProductImage image = new ProductImage();
                image.setProductId(product.getId());
                image.setVariantId(savedVariantId);
                image.setImagePath(path);
                image.setIsPrimary(Boolean.TRUE.equals(imageReq.getPrimary()) && sort == 0);
                image.setSortOrder(imageReq.getSortOrder() != null ? imageReq.getSortOrder() : sort++);
                image.setCreatedAt(now);
                productImageRepository.save(image);
            }
        }

        product.setUpdatedAt(now);
        productRepository.save(product);

        return CreateProductResponse.CreatedVariantRef.builder()
                .variantId(variant.getId())
                .clientKey(String.valueOf(variant.getId()))
                .build();
    }

    private Product requireProduct(Long sellerId, Long productId) {
        return productRepository.findByIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
    }
}

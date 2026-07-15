package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.dto.CreateProductImageRequest;
import com.ecommerce.sellerbackend.dto.CreateProductResponse;
import com.ecommerce.sellerbackend.dto.DeliveryWeightSlabResponse;
import com.ecommerce.sellerbackend.dto.UpdateProductRequest;
import com.ecommerce.sellerbackend.dto.UpdateProductVariantRequest;
import com.ecommerce.sellerbackend.entity.Product;
import com.ecommerce.sellerbackend.entity.ProductImage;
import com.ecommerce.sellerbackend.entity.ProductVariant;
import com.ecommerce.sellerbackend.entity.Subcategory;
import com.ecommerce.sellerbackend.exception.ResourceNotFoundException;
import com.ecommerce.sellerbackend.repository.ProductImageRepository;
import com.ecommerce.sellerbackend.repository.ProductRepository;
import com.ecommerce.sellerbackend.repository.ProductVariantRepository;
import com.ecommerce.sellerbackend.repository.SubcategoryRepository;
import com.ecommerce.sellerbackend.service.AdminSettingsLookupService;
import com.ecommerce.sellerbackend.service.DeliverySlabLookupService;
import com.ecommerce.sellerbackend.service.ProductMediaStorageService;
import com.ecommerce.sellerbackend.service.support.ProductCatalogResolver;
import com.ecommerce.sellerbackend.service.support.ProductReapprovalSupport;
import com.ecommerce.sellerbackend.service.support.ProductSkuGenerator;
import com.ecommerce.sellerbackend.service.support.ProductSpecificationsCodec;
import com.ecommerce.sellerbackend.service.support.ProductVariantPricingCalculator;
import com.ecommerce.sellerbackend.service.support.ProductVariantPricingCalculator.VariantPricing;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductUpdateService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final ProductCatalogResolver catalogResolver;
    private final ProductMediaStorageService productMediaStorageService;
    private final DeliverySlabLookupService deliverySlabLookupService;
    private final AdminSettingsLookupService adminSettingsLookupService;

    @Transactional
    public CreateProductResponse update(Long sellerId, Long productId, UpdateProductRequest request) {
        Product product = productRepository.findByIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));

        ProductCatalogResolver.CategorySubcategoryIds ids =
                catalogResolver.resolveCategoryIds(request.toCreatePayload());
        Subcategory subcategory = subcategoryRepository.findById(ids.subcategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Subcategory not found."));

        BigDecimal gstPercent = request.getGstPercentage() != null
                ? request.getGstPercentage()
                : subcategory.getGstPercentage() != null
                ? subcategory.getGstPercentage()
                : ProductVariantPricingCalculator.DEFAULT_GST;

        LocalDateTime now = LocalDateTime.now();
        DeliveryWeightSlabResponse slab = deliverySlabLookupService.resolveForWeight(request.getProductWeight());
        BigDecimal intraCity = slab.getIntraCityCharge();
        BigDecimal metroMetro = slab.getMetroMetroCharge();
        BigDecimal commissionPercent = adminSettingsLookupService.getSellerCommissionPercent(sellerId);

        product.setCategoryId(ids.categoryId());
        product.setSubcategoryId(ids.subcategoryId());
        product.setSizeChartId(request.getSizeChartId());
        product.setName(request.getName().trim());
        if (request.getSku() != null && !request.getSku().isBlank()) {
            product.setSku(request.getSku().trim());
        }
        product.setHsnCode(request.getHsnCode().trim());
        product.setProductMaterialType(request.getProductMaterialType());
        product.setGstPercentage(gstPercent);
        product.setLengthCm(request.getLengthCm());
        product.setWidthCm(request.getWidthCm());
        product.setHeightCm(request.getHeightCm());
        product.setProductWeight(request.getProductWeight());
        product.setFragile(Boolean.TRUE.equals(request.getFragile()));
        product.setShortDescription(request.getShortDescription());
        product.setDescription(request.getDescription());
        product.setFeatures(request.getFeatures());
        product.setReturnPolicy(request.getReturnPolicy());
        product.setSpecifications(ProductSpecificationsCodec.encode(request.toCreatePayload(), request.getSpecifications()));
        product.setIntraCityCharge(intraCity);
        product.setMetroMetroCharge(metroMetro);
        product.setDeliveryTimeMin(request.getDeliveryTimeMin());
        product.setDeliveryTimeMax(request.getDeliveryTimeMax());
        product.setDeliveryInfo(request.getDeliveryInfo());
        product.setWarrantyInfo(request.getWarrantyInfo());
        product.setCareInstructions(request.getCareInstructions());
        if (request.getAcceptCod() != null) {
            product.setAcceptCod(request.getAcceptCod());
        }
        ProductReapprovalSupport.markPendingIfNeedsReapproval(product);
        product.setUpdatedAt(now);
        productRepository.save(product);

        Set<Long> keepVariantIds = new HashSet<>();
        Map<String, Long> variantIdsByClientKey = new HashMap<>();
        List<CreateProductResponse.CreatedVariantRef> refs = new ArrayList<>();

        int variantIndex = 0;
        for (UpdateProductVariantRequest variantReq : request.getVariants()) {
            variantIndex++;
            if (Boolean.TRUE.equals(variantReq.getRemove()) && variantReq.getId() != null) {
                productVariantRepository.deleteById(variantReq.getId());
                continue;
            }

            String clientKey = firstNonBlank(variantReq.getClientKey(), "v" + variantIndex);

            ProductVariant existingVariant = null;
            if (variantReq.getId() != null) {
                existingVariant = productVariantRepository.findByIdAndProductId(variantReq.getId(), productId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Variant not found: " + variantReq.getId()));
            }

            String existingColor = existingVariant != null ? existingVariant.getColor() : null;
            String existingSize = existingVariant != null ? existingVariant.getSize() : null;
            String colorId = catalogResolver.resolveColorId(sellerId, variantReq, existingColor);
            String sizeId = catalogResolver.resolveSizeId(sellerId, variantReq, existingSize);
            VariantPricing pricing = ProductVariantPricingCalculator.calculate(
                    variantReq.getMrp(),
                    variantReq.getSellingPrice(),
                    variantReq.getDiscount(),
                    gstPercent,
                    intraCity,
                    metroMetro,
                    commissionPercent);

            String colorName = variantReq.getColor() != null ? variantReq.getColor().trim() : "";
            String sizeName = variantReq.getSize() != null ? variantReq.getSize().trim() : "";
            String generatedSku = ProductSkuGenerator.generateVariantSku(request.getName(), colorName, sizeName);

            ProductVariant variant;
            if (existingVariant != null) {
                variant = existingVariant;
            } else {
                variant = new ProductVariant();
                variant.setProductId(productId);
                variant.setCreatedAt(now);
            }

            variant.setColor(colorId);
            variant.setSize(sizeId);
            variant.setSku(firstNonBlank(variantReq.getSku(), generatedSku));
            variant.setStock(variantReq.getStock());
            variant.setMinQuantity(variantReq.getMinQuantity());
            variant.setBasePrice(variantReq.getSellingPrice());
            variant.setMrpExclGst(variantReq.getMrp());
            variant.setMrpPrice(pricing.finalPrice());
            variant.setDiscountPercentage(pricing.discountPercentage());
            variant.setDiscountAmount(pricing.discountAmount());
            variant.setSellingPrice(variantReq.getSellingPrice());
            variant.setTaxPercentage(gstPercent);
            variant.setTaxAmount(pricing.taxAmount());
            variant.setFinalPrice(pricing.finalPrice());
            variant.setMrpInclGst(pricing.mrpInclGst());
            variant.setIntraCityDeliveryCharge(intraCity);
            variant.setMetroMetroDeliveryCharge(metroMetro);
            variant.setCommissionPercentage(commissionPercent);
            variant.setCommissionAmount(pricing.commissionAmount());
            variant.setTotalPriceIntraCity(pricing.totalIntraCity());
            variant.setTotalPriceMetroMetro(pricing.totalMetroMetro());
            variant.setVideoPath(variantReq.getVideoUrl());
            variant.setWeight(request.getProductWeight());
            variant.setUpdatedAt(now);

            variant = productVariantRepository.save(variant);
            final Long savedVariantId = variant.getId();
            keepVariantIds.add(savedVariantId);
            variantIdsByClientKey.put(clientKey, savedVariantId);
            refs.add(CreateProductResponse.CreatedVariantRef.builder()
                    .clientKey(clientKey)
                    .variantId(savedVariantId)
                    .build());

            if (variantReq.getImages() != null && !variantReq.getImages().isEmpty()) {
                productImageRepository.findByProductId(productId).stream()
                        .filter(img -> savedVariantId.equals(img.getVariantId()))
                        .forEach(img -> productImageRepository.deleteById(img.getId()));
                saveVariantImages(productId, savedVariantId, variantReq.getImages());
            }
        }

        List<ProductVariant> existing = productVariantRepository.findByProductIdOrderByIdAsc(productId);
        for (ProductVariant existingVariant : existing) {
            if (!keepVariantIds.contains(existingVariant.getId())) {
                productImageRepository.findByProductId(productId).stream()
                        .filter(img -> existingVariant.getId().equals(img.getVariantId()))
                        .forEach(img -> productImageRepository.deleteById(img.getId()));
                productVariantRepository.delete(existingVariant);
            }
        }

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            productImageRepository.findByProductId(productId).stream()
                    .filter(img -> img.getVariantId() == null)
                    .forEach(img -> productImageRepository.deleteById(img.getId()));
            saveProductImages(productId, request.getImages(), variantIdsByClientKey);
        }

        List<ProductVariant> remaining = productVariantRepository.findByProductIdOrderByIdAsc(productId);
        if (!remaining.isEmpty()) {
            ProductVariant primary = remaining.get(0);
            if (primary.getSku() != null && !primary.getSku().isBlank()) {
                product.setSku(primary.getSku());
                productRepository.save(product);
            }
        }

        return CreateProductResponse.builder()
                .productId(productId)
                .variants(refs)
                .build();
    }

    private void saveProductImages(
            Long productId,
            List<CreateProductImageRequest> images,
            Map<String, Long> variantIdsByClientKey) {
        int sort = 0;
        for (CreateProductImageRequest imageReq : images) {
            Long variantId = null;
            if (imageReq.getVariantClientKey() != null && !imageReq.getVariantClientKey().isBlank()) {
                variantId = variantIdsByClientKey.get(imageReq.getVariantClientKey());
            }
            boolean primary = Boolean.TRUE.equals(imageReq.getPrimary());
            int order = imageReq.getSortOrder() != null ? imageReq.getSortOrder() : sort++;
            persistImage(productId, variantId, imageReq.getSource(), primary, order);
        }
    }

    private void saveVariantImages(Long productId, Long variantId, List<CreateProductImageRequest> images) {
        int sort = 0;
        for (CreateProductImageRequest imageReq : images) {
            int order = imageReq.getSortOrder() != null ? imageReq.getSortOrder() : sort++;
            boolean primary = Boolean.TRUE.equals(imageReq.getPrimary()) && order == 0;
            persistImage(productId, variantId, imageReq.getSource(), primary, order);
        }
    }

    private void persistImage(Long productId, Long variantId, String source, boolean primary, int sortOrder) {
        if (source == null || source.isBlank()) {
            return;
        }
        String path = productMediaStorageService.storeProductImage(source);
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Failed to store product image.");
        }
        ProductImage image = new ProductImage();
        image.setProductId(productId);
        image.setVariantId(variantId);
        image.setImagePath(path);
        image.setIsPrimary(primary);
        image.setSortOrder(sortOrder);
        image.setCreatedAt(LocalDateTime.now());
        productImageRepository.save(image);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}

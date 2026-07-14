package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.dto.CreateProductImageRequest;
import com.ecommerce.sellerbackend.dto.CreateProductRequest;
import com.ecommerce.sellerbackend.dto.CreateProductResponse;
import com.ecommerce.sellerbackend.dto.CreateProductVariantRequest;
import com.ecommerce.sellerbackend.dto.DeliveryWeightSlabResponse;
import com.ecommerce.sellerbackend.entity.Color;
import com.ecommerce.sellerbackend.entity.Product;
import com.ecommerce.sellerbackend.entity.ProductImage;
import com.ecommerce.sellerbackend.entity.ProductVariant;
import com.ecommerce.sellerbackend.entity.Size;
import com.ecommerce.sellerbackend.entity.Subcategory;
import com.ecommerce.sellerbackend.repository.ColorRepository;
import com.ecommerce.sellerbackend.repository.ProductImageRepository;
import com.ecommerce.sellerbackend.repository.ProductRepository;
import com.ecommerce.sellerbackend.repository.ProductVariantRepository;
import com.ecommerce.sellerbackend.repository.SizeRepository;
import com.ecommerce.sellerbackend.repository.SubcategoryRepository;
import com.ecommerce.sellerbackend.service.DeliverySlabLookupService;
import com.ecommerce.sellerbackend.service.ProductMediaStorageService;
import com.ecommerce.sellerbackend.service.support.ProductCatalogResolver;
import com.ecommerce.sellerbackend.service.support.ProductSkuGenerator;
import com.ecommerce.sellerbackend.service.support.ProductSpecificationsCodec;
import com.ecommerce.sellerbackend.service.support.ProductVariantPricingCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
@RequiredArgsConstructor
public class ProductCreateService {

    private static final BigDecimal DEFAULT_GST = new BigDecimal("5.00");
    private static final BigDecimal COMMISSION_PERCENT = BigDecimal.ZERO;
    private static final BigDecimal DEFAULT_INTRA_CITY = new BigDecimal("175.00");
    private static final BigDecimal DEFAULT_METRO_METRO = new BigDecimal("205.00");

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductCatalogResolver catalogResolver;
    private final SubcategoryRepository subcategoryRepository;
    private final ColorRepository colorRepository;
    private final SizeRepository sizeRepository;
    private final ProductMediaStorageService productMediaStorageService;
    private final DeliverySlabLookupService deliverySlabLookupService;

    @Transactional
    public CreateProductResponse create(Long sellerId, CreateProductRequest request) {
        ProductCatalogResolver.CategorySubcategoryIds ids = catalogResolver.resolveCategoryIds(request);
        Subcategory subcategory = subcategoryRepository.findById(ids.subcategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Subcategory not found."));

        BigDecimal gstPercent = request.getGstPercentage() != null
                ? request.getGstPercentage()
                : subcategory.getGstPercentage() != null
                ? subcategory.getGstPercentage()
                : DEFAULT_GST;

        LocalDateTime now = LocalDateTime.now();
        DeliveryWeightSlabResponse slab = deliverySlabLookupService.resolveForWeight(request.getProductWeight());
        BigDecimal intraCity = slab.getIntraCityCharge();
        BigDecimal metroMetro = slab.getMetroMetroCharge();

        Product product = new Product();
        product.setSellerId(sellerId);
        product.setCategoryId(ids.categoryId());
        product.setSubcategoryId(ids.subcategoryId());
        product.setSizeChartId(request.getSizeChartId());
        product.setName(request.getName().trim());
        product.setSku(firstNonBlank(request.getSku()));
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
        product.setSpecifications(ProductSpecificationsCodec.encode(request, request.getSpecifications()));
        product.setStatus("pending");
        product.setDeliveryTimeMin(request.getDeliveryTimeMin());
        product.setDeliveryTimeMax(request.getDeliveryTimeMax());
        product.setDeliveryInfo(request.getDeliveryInfo());
        product.setWarrantyInfo(request.getWarrantyInfo());
        product.setCareInstructions(request.getCareInstructions());
        product.setIntraCityCharge(intraCity);
        product.setMetroMetroCharge(metroMetro);
        product.setAcceptCod(request.getAcceptCod() == null || request.getAcceptCod());
        product.setDeliverAllLocations(true);
        product.setCreatedAt(now);
        product.setUpdatedAt(now);

        if (product.getSku() == null || product.getSku().isBlank()) {
            product.setSku(generateSku(request));
        }

        product = productRepository.save(product);

        Map<String, Long> variantIdsByClientKey = new HashMap<>();
        List<CreateProductResponse.CreatedVariantRef> createdVariants = new ArrayList<>();
        ProductVariant firstSavedVariant = null;

        int variantIndex = 0;
        for (CreateProductVariantRequest variantReq : request.getVariants()) {
            variantIndex++;
            String clientKey = firstNonBlank(variantReq.getClientKey(), "v" + variantIndex);

            String colorId = resolveColorId(sellerId, variantReq);
            String sizeId = resolveSizeId(sellerId, variantReq);

            VariantPricing pricing = calculateVariantPricing(
                    variantReq.getMrp(),
                    variantReq.getSellingPrice(),
                    variantReq.getDiscount(),
                    gstPercent,
                    intraCity,
                    metroMetro);

            String colorName = variantReq.getColor() != null ? variantReq.getColor().trim() : "";
            String sizeName = variantReq.getSize() != null ? variantReq.getSize().trim() : "";
            String generatedSku = ProductSkuGenerator.generateVariantSku(request.getName(), colorName, sizeName);

            ProductVariant variant = new ProductVariant();
            variant.setProductId(product.getId());
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
            variant.setCommissionPercentage(COMMISSION_PERCENT);
            variant.setCommissionAmount(pricing.commissionAmount());
            variant.setTotalPriceIntraCity(pricing.totalIntraCity());
            variant.setTotalPriceMetroMetro(pricing.totalMetroMetro());
            variant.setVideoPath(variantReq.getVideoUrl());
            variant.setWeight(request.getProductWeight());
            variant.setCreatedAt(now);
            variant.setUpdatedAt(now);

            variant = productVariantRepository.save(variant);
            if (firstSavedVariant == null) {
                firstSavedVariant = variant;
            }
            variantIdsByClientKey.put(clientKey, variant.getId());
            createdVariants.add(CreateProductResponse.CreatedVariantRef.builder()
                    .clientKey(clientKey)
                    .variantId(variant.getId())
                    .build());

            saveVariantImages(product.getId(), variant.getId(), variantReq.getImages());
        }

        if (firstSavedVariant != null && firstSavedVariant.getSku() != null && !firstSavedVariant.getSku().isBlank()) {
            product.setSku(firstSavedVariant.getSku());
            productRepository.save(product);
        }

        saveProductLevelImages(product.getId(), request.getImages(), variantIdsByClientKey);

        List<ProductImage> savedImages = productImageRepository.findByProductId(product.getId());
        boolean requestHadImageSources = (request.getImages() != null && request.getImages().stream()
                .anyMatch(img -> img.getSource() != null && !img.getSource().isBlank()))
                || (request.getVariants() != null && request.getVariants().stream()
                .anyMatch(v -> v.getImages() != null && v.getImages().stream()
                        .anyMatch(img -> img.getSource() != null && !img.getSource().isBlank())));
        if (requestHadImageSources && savedImages.isEmpty()) {
            throw new IllegalArgumentException(
                    "Product images could not be saved. Please re-select images and try again.");
        }

        return CreateProductResponse.builder()
                .productId(product.getId())
                .variants(createdVariants)
                .build();
    }

    private void saveProductLevelImages(
            Long productId,
            List<CreateProductImageRequest> images,
            Map<String, Long> variantIdsByClientKey) {
        if (images == null || images.isEmpty()) {
            return;
        }
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
        if (images == null || images.isEmpty()) {
            return;
        }
        int sort = 0;
        for (CreateProductImageRequest imageReq : images) {
            int order = imageReq.getSortOrder() != null ? imageReq.getSortOrder() : sort++;
            boolean primary = Boolean.TRUE.equals(imageReq.getPrimary()) && sort == 1;
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

    private String resolveColorId(Long sellerId, CreateProductVariantRequest variantReq) {
        if (variantReq.getColorId() != null) {
            return String.valueOf(variantReq.getColorId());
        }
        String raw = variantReq.getColor() != null ? variantReq.getColor().trim() : "";
        if (!raw.isEmpty() && raw.matches("\\d+")) {
            Long parsedId = Long.parseLong(raw);
            return colorRepository.findVisibleByIdForSeller(parsedId, sellerId)
                    .map(color -> String.valueOf(color.getId()))
                    .orElseThrow(() -> new IllegalArgumentException("Color not found: " + raw));
        }
        Color color = colorRepository.findVisibleByNameForSeller(sellerId, variantReq.getColor())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Color not found: " + variantReq.getColor()));
        return String.valueOf(color.getId());
    }

    private String resolveSizeId(Long sellerId, CreateProductVariantRequest variantReq) {
        if (variantReq.getSizeId() != null) {
            return String.valueOf(variantReq.getSizeId());
        }
        String raw = variantReq.getSize() != null ? variantReq.getSize().trim() : "";
        if (!raw.isEmpty() && raw.matches("\\d+")) {
            Long parsedId = Long.parseLong(raw);
            return sizeRepository.findVisibleByIdForSeller(parsedId, sellerId)
                    .map(size -> String.valueOf(size.getId()))
                    .orElseThrow(() -> new IllegalArgumentException("Size not found: " + raw));
        }
        Size size = sizeRepository.findVisibleByNameOrCodeForSeller(sellerId, variantReq.getSize())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Size not found: " + variantReq.getSize()));
        return String.valueOf(size.getId());
    }

    private VariantPricing calculateVariantPricing(
            BigDecimal mrpExcl,
            BigDecimal sellingExcl,
            BigDecimal discountOverride,
            BigDecimal gstPercent,
            BigDecimal intraCity,
            BigDecimal metroMetro) {
        return VariantPricing.from(ProductVariantPricingCalculator.calculate(
                mrpExcl, sellingExcl, discountOverride, gstPercent, intraCity, metroMetro));
    }

    private VariantPricing calculateVariantPricing(
            BigDecimal mrpExcl,
            BigDecimal sellingExcl,
            BigDecimal discountOverride,
            BigDecimal gstPercent) {
        return calculateVariantPricing(
                mrpExcl, sellingExcl, discountOverride, gstPercent, DEFAULT_INTRA_CITY, DEFAULT_METRO_METRO);
    }

    private String generateSku(CreateProductRequest request) {
        String base = request.getName()
                .trim()
                .toUpperCase()
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (base.length() > 20) {
            base = base.substring(0, 20);
        }
        return "SKU-" + base + "-" + System.currentTimeMillis() % 100000;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private record VariantPricing(
            BigDecimal discountPercentage,
            BigDecimal discountAmount,
            BigDecimal taxAmount,
            BigDecimal finalPrice,
            BigDecimal mrpInclGst,
            BigDecimal commissionAmount,
            BigDecimal totalIntraCity,
            BigDecimal totalMetroMetro) {

        private static VariantPricing from(ProductVariantPricingCalculator.VariantPricing pricing) {
            return new VariantPricing(
                    pricing.discountPercentage(),
                    pricing.discountAmount(),
                    pricing.taxAmount(),
                    pricing.finalPrice(),
                    pricing.mrpInclGst(),
                    pricing.commissionAmount(),
                    pricing.totalIntraCity(),
                    pricing.totalMetroMetro());
        }
    }
}

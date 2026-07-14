package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.dto.product.CreateProductImageRequest;
import com.ecommerce.adminbackend.dto.product.CreateProductRequest;
import com.ecommerce.adminbackend.dto.product.CreateProductVariantRequest;
import com.ecommerce.adminbackend.dto.product.UpdateProductRequest;
import com.ecommerce.adminbackend.dto.product.UpdateProductVariantRequest;
import com.ecommerce.adminbackend.entity.DeliveryWeightSlab;
import com.ecommerce.adminbackend.entity.Product;
import com.ecommerce.adminbackend.entity.ProductImage;
import com.ecommerce.adminbackend.entity.ProductVariant;
import com.ecommerce.adminbackend.entity.Size;
import com.ecommerce.adminbackend.entity.Subcategory;
import com.ecommerce.adminbackend.exception.ResourceNotFoundException;
import com.ecommerce.adminbackend.repository.CategoryRepository;
import com.ecommerce.adminbackend.repository.ColorRepository;
import com.ecommerce.adminbackend.repository.DeliveryWeightSlabRepository;
import com.ecommerce.adminbackend.repository.ProductImageRepository;
import com.ecommerce.adminbackend.repository.ProductRepository;
import com.ecommerce.adminbackend.repository.ProductVariantRepository;
import com.ecommerce.adminbackend.repository.SizeRepository;
import com.ecommerce.adminbackend.repository.SubcategoryRepository;
import com.ecommerce.adminbackend.service.ProductMediaStorageService;
import com.ecommerce.adminbackend.service.support.ProductVariantPricingCalculator;
import com.ecommerce.adminbackend.service.support.ProductVariantPricingCalculator.VariantPricing;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductAdminMutationService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final ColorRepository colorRepository;
    private final SizeRepository sizeRepository;
    private final DeliveryWeightSlabRepository deliveryWeightSlabRepository;
    private final ProductMediaStorageService productMediaStorageService;

    @Transactional
    public Map<String, Object> create(CreateProductRequest request) {
        ResolvedCatalog catalog = resolveCatalog(request.getCategoryId(), request.getCategoryName(),
                request.getSubcategoryId(), request.getSubcategoryName());
        BigDecimal gstPercent = resolveGst(request.getGstPercentage(), catalog.subcategory());
        LocalDateTime now = LocalDateTime.now();
        DeliveryCharges charges = resolveDeliveryCharges(request.getProductWeight());

        Product product = new Product();
        product.setSellerId(null);
        applyProductFields(product, request.getName(), request.getSku(), request.getHsnCode(),
                request.getProductMaterialType(), gstPercent, request.getLengthCm(), request.getWidthCm(),
                request.getHeightCm(), request.getProductWeight(), request.getFragile(),
                request.getShortDescription(), request.getDescription(), request.getFeatures(),
                request.getReturnPolicy(), request.getSpecifications(), request.getSizeChartId(),
                request.getDeliveryTimeMin(), request.getDeliveryTimeMax(), request.getDeliveryInfo(),
                request.getWarrantyInfo(), request.getCareInstructions(), request.getAcceptCod(),
                catalog.categoryId(), catalog.subcategoryId(), charges, now);
        product.setStatus("active");
        product.setReviewedAt(now);
        product.setCreatedAt(now);
        product = productRepository.save(product);

        Map<String, Long> variantIdsByClientKey = new HashMap<>();
        List<Map<String, Object>> createdVariants = new ArrayList<>();
        ProductVariant firstVariant = null;

        int variantIndex = 0;
        for (CreateProductVariantRequest variantReq : request.getVariants()) {
            variantIndex++;
            String clientKey = firstNonBlank(variantReq.getClientKey(), "v" + variantIndex);
            ProductVariant variant = buildVariant(product.getId(), variantReq, request.getName(),
                    request.getProductWeight(), gstPercent, charges, now, null);
            variant = productVariantRepository.save(variant);
            if (firstVariant == null) {
                firstVariant = variant;
            }
            variantIdsByClientKey.put(clientKey, variant.getId());
            createdVariants.add(variantRef(clientKey, variant.getId()));
            saveVariantImages(product.getId(), variant.getId(), variantReq.getImages());
        }

        if (firstVariant != null && firstVariant.getSku() != null && !firstVariant.getSku().isBlank()) {
            product.setSku(firstVariant.getSku());
            productRepository.save(product);
        }

        saveProductLevelImages(product.getId(), request.getImages(), variantIdsByClientKey);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("productId", product.getId());
        response.put("status", product.getStatus());
        response.put("variants", createdVariants);
        response.put("message", "Product created.");
        return response;
    }

    @Transactional
    public Map<String, Object> update(Long productId, UpdateProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));

        ResolvedCatalog catalog = resolveCatalog(request.getCategoryId(), request.getCategoryName(),
                request.getSubcategoryId(), request.getSubcategoryName());
        BigDecimal gstPercent = resolveGst(request.getGstPercentage(), catalog.subcategory());
        LocalDateTime now = LocalDateTime.now();
        DeliveryCharges charges = resolveDeliveryCharges(request.getProductWeight());

        applyProductFields(product, request.getName(), request.getSku(), request.getHsnCode(),
                request.getProductMaterialType(), gstPercent, request.getLengthCm(), request.getWidthCm(),
                request.getHeightCm(), request.getProductWeight(), request.getFragile(),
                request.getShortDescription(), request.getDescription(), request.getFeatures(),
                request.getReturnPolicy(), request.getSpecifications(), request.getSizeChartId(),
                request.getDeliveryTimeMin(), request.getDeliveryTimeMax(), request.getDeliveryInfo(),
                request.getWarrantyInfo(), request.getCareInstructions(), request.getAcceptCod(),
                catalog.categoryId(), catalog.subcategoryId(), charges, now);
        productRepository.save(product);

        Set<Long> keepVariantIds = new HashSet<>();
        Map<String, Long> variantIdsByClientKey = new HashMap<>();
        List<Map<String, Object>> refs = new ArrayList<>();

        int variantIndex = 0;
        for (UpdateProductVariantRequest variantReq : request.getVariants()) {
            variantIndex++;
            if (Boolean.TRUE.equals(variantReq.getRemove()) && variantReq.getId() != null) {
                deleteVariantImages(productId, variantReq.getId());
                productVariantRepository.deleteById(variantReq.getId());
                continue;
            }

            String clientKey = firstNonBlank(variantReq.getClientKey(), "v" + variantIndex);
            ProductVariant existing = null;
            if (variantReq.getId() != null) {
                existing = productVariantRepository.findById(variantReq.getId())
                        .filter(v -> productId.equals(v.getProductId()))
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Variant not found: " + variantReq.getId()));
            }

            ProductVariant variant = buildVariant(productId, variantReq, request.getName(),
                    request.getProductWeight(), gstPercent, charges, now, existing);
            variant = productVariantRepository.save(variant);
            keepVariantIds.add(variant.getId());
            variantIdsByClientKey.put(clientKey, variant.getId());
            refs.add(variantRef(clientKey, variant.getId()));

            if (variantReq.getImages() != null && !variantReq.getImages().isEmpty()) {
                deleteVariantImages(productId, variant.getId());
                saveVariantImages(productId, variant.getId(), variantReq.getImages());
            }
        }

        for (ProductVariant existingVariant : productVariantRepository.findByProductIdOrderByIdAsc(productId)) {
            if (!keepVariantIds.contains(existingVariant.getId())) {
                deleteVariantImages(productId, existingVariant.getId());
                productVariantRepository.delete(existingVariant);
            }
        }

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            productImageRepository.findByProductIdOrderBySortOrderAsc(productId).stream()
                    .filter(img -> img.getVariantId() == null)
                    .forEach(img -> productImageRepository.deleteById(img.getId()));
            saveProductLevelImages(productId, request.getImages(), variantIdsByClientKey);
        }

        List<ProductVariant> remaining = productVariantRepository.findByProductIdOrderByIdAsc(productId);
        if (!remaining.isEmpty() && remaining.get(0).getSku() != null && !remaining.get(0).getSku().isBlank()) {
            product.setSku(remaining.get(0).getSku());
            productRepository.save(product);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("productId", productId);
        response.put("status", product.getStatus());
        response.put("variants", refs);
        response.put("message", "Product updated.");
        return response;
    }

    @Transactional
    public void delete(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));

        List<ProductImage> images = productImageRepository.findByProductIdOrderBySortOrderAsc(productId);
        images.forEach(img -> productImageRepository.deleteById(img.getId()));

        List<ProductVariant> variants = productVariantRepository.findByProductIdOrderByIdAsc(productId);
        variants.forEach(productVariantRepository::delete);

        try {
            productRepository.delete(product);
        } catch (DataIntegrityViolationException ex) {
            product.setStatus("inactive");
            product.setUpdatedAt(LocalDateTime.now());
            productRepository.save(product);
        }
    }

    private void applyProductFields(
            Product product,
            String name,
            String sku,
            String hsnCode,
            String materialType,
            BigDecimal gstPercent,
            BigDecimal lengthCm,
            BigDecimal widthCm,
            BigDecimal heightCm,
            BigDecimal productWeight,
            Boolean fragile,
            String shortDescription,
            String description,
            String features,
            String returnPolicy,
            String specifications,
            Integer sizeChartId,
            Integer deliveryTimeMin,
            Integer deliveryTimeMax,
            String deliveryInfo,
            String warrantyInfo,
            String careInstructions,
            Boolean acceptCod,
            Integer categoryId,
            Integer subcategoryId,
            DeliveryCharges charges,
            LocalDateTime now) {
        product.setCategoryId(categoryId);
        product.setSubcategoryId(subcategoryId);
        product.setSizeChartId(sizeChartId);
        product.setName(name.trim());
        if (sku != null && !sku.isBlank()) {
            product.setSku(sku.trim());
        } else if (product.getSku() == null || product.getSku().isBlank()) {
            product.setSku(generateSku(name));
        }
        product.setHsnCode(hsnCode.trim());
        product.setProductMaterialType(materialType);
        product.setGstPercentage(gstPercent);
        product.setLengthCm(lengthCm);
        product.setWidthCm(widthCm);
        product.setHeightCm(heightCm);
        product.setProductWeight(productWeight);
        product.setFragile(Boolean.TRUE.equals(fragile));
        product.setShortDescription(shortDescription);
        product.setDescription(description);
        product.setFeatures(features);
        product.setReturnPolicy(returnPolicy);
        product.setSpecifications(specifications);
        product.setDeliveryTimeMin(deliveryTimeMin);
        product.setDeliveryTimeMax(deliveryTimeMax);
        product.setDeliveryInfo(deliveryInfo);
        product.setWarrantyInfo(warrantyInfo);
        product.setCareInstructions(careInstructions);
        product.setIntraCityCharge(charges.intraCity());
        product.setMetroMetroCharge(charges.metroMetro());
        product.setAcceptCod(acceptCod == null || acceptCod);
        product.setDeliverAllLocations(true);
        product.setUpdatedAt(now);
    }

    private ProductVariant buildVariant(
            Long productId,
            CreateProductVariantRequest variantReq,
            String productName,
            BigDecimal productWeight,
            BigDecimal gstPercent,
            DeliveryCharges charges,
            LocalDateTime now,
            ProductVariant existing) {
        String colorId = resolveColorId(variantReq);
        String sizeId = resolveSizeId(variantReq);
        VariantPricing pricing = ProductVariantPricingCalculator.calculate(
                variantReq.getMrp(),
                variantReq.getSellingPrice(),
                variantReq.getDiscount(),
                gstPercent,
                charges.intraCity(),
                charges.metroMetro());

        String colorName = variantReq.getColor() != null ? variantReq.getColor().trim() : "";
        String sizeName = variantReq.getSize() != null ? variantReq.getSize().trim() : "";
        String generatedSku = generateVariantSku(productName, colorName, sizeName);

        ProductVariant variant = existing != null ? existing : new ProductVariant();
        if (existing == null) {
            variant.setProductId(productId);
            variant.setCreatedAt(now);
        }
        variant.setColor(colorId);
        variant.setSize(sizeId);
        variant.setSku(firstNonBlank(variantReq.getSku(), generatedSku));
        variant.setStock(variantReq.getStock());
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
        variant.setIntraCityDeliveryCharge(charges.intraCity());
        variant.setMetroMetroDeliveryCharge(charges.metroMetro());
        variant.setCommissionPercentage(ProductVariantPricingCalculator.COMMISSION_PERCENT);
        variant.setCommissionAmount(pricing.commissionAmount());
        variant.setTotalPriceIntraCity(pricing.totalIntraCity());
        variant.setTotalPriceMetroMetro(pricing.totalMetroMetro());
        variant.setVideoPath(variantReq.getVideoUrl());
        variant.setWeight(productWeight);
        variant.setUpdatedAt(now);
        return variant;
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
            boolean primary = Boolean.TRUE.equals(imageReq.getPrimary()) && order == 0;
            persistImage(productId, variantId, imageReq.getSource(), primary, order);
        }
    }

    private void deleteVariantImages(Long productId, Long variantId) {
        productImageRepository.findByProductIdOrderBySortOrderAsc(productId).stream()
                .filter(img -> variantId.equals(img.getVariantId()))
                .forEach(img -> productImageRepository.deleteById(img.getId()));
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

    private ResolvedCatalog resolveCatalog(
            Integer categoryId,
            String categoryName,
            Integer subcategoryId,
            String subcategoryName) {
        Integer resolvedCategoryId = categoryId;
        if (resolvedCategoryId == null && categoryName != null && !categoryName.isBlank()) {
            resolvedCategoryId = categoryRepository.findAll().stream()
                    .filter(c -> categoryName.trim().equalsIgnoreCase(c.getCategoryName()))
                    .map(c -> c.getId())
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryName));
        }
        if (resolvedCategoryId == null) {
            throw new IllegalArgumentException("Category is required.");
        }
        categoryRepository.findById(resolvedCategoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found."));

        Integer resolvedSubcategoryId = subcategoryId;
        if (resolvedSubcategoryId == null && subcategoryName != null && !subcategoryName.isBlank()) {
            final Integer catId = resolvedCategoryId;
            resolvedSubcategoryId = subcategoryRepository
                    .findByCategoryIdOrderBySubcategoryNameAsc(catId)
                    .stream()
                    .filter(s -> subcategoryName.trim().equalsIgnoreCase(s.getSubcategoryName()))
                    .map(Subcategory::getId)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Subcategory not found: " + subcategoryName));
        }
        if (resolvedSubcategoryId == null) {
            throw new IllegalArgumentException("Subcategory is required.");
        }
        Subcategory subcategory = subcategoryRepository.findById(resolvedSubcategoryId)
                .orElseThrow(() -> new IllegalArgumentException("Subcategory not found."));
        if (!resolvedCategoryId.equals(subcategory.getCategoryId())) {
            throw new IllegalArgumentException("Subcategory does not belong to the selected category.");
        }
        return new ResolvedCatalog(resolvedCategoryId, resolvedSubcategoryId, subcategory);
    }

    private BigDecimal resolveGst(BigDecimal requestGst, Subcategory subcategory) {
        if (requestGst != null) {
            return requestGst;
        }
        if (subcategory.getGstPercentage() != null) {
            return subcategory.getGstPercentage();
        }
        return ProductVariantPricingCalculator.DEFAULT_GST;
    }

    private DeliveryCharges resolveDeliveryCharges(BigDecimal weightKg) {
        BigDecimal weight = weightKg != null ? weightKg : BigDecimal.ZERO;
        List<DeliveryWeightSlab> slabs = deliveryWeightSlabRepository.findAllByOrderBySortOrderAscIdAsc();
        for (DeliveryWeightSlab slab : slabs) {
            if (!Boolean.TRUE.equals(slab.getActive())) {
                continue;
            }
            if (weight.compareTo(slab.getMinWeightKg()) >= 0
                    && weight.compareTo(slab.getMaxWeightKg()) <= 0) {
                return new DeliveryCharges(slab.getIntraCityCharge(), slab.getMetroMetroCharge());
            }
        }
        return new DeliveryCharges(
                ProductVariantPricingCalculator.DEFAULT_INTRA_CITY,
                ProductVariantPricingCalculator.DEFAULT_METRO_METRO);
    }

    private String resolveColorId(CreateProductVariantRequest variantReq) {
        if (variantReq.getColorId() != null) {
            return String.valueOf(variantReq.getColorId());
        }
        String raw = variantReq.getColor() != null ? variantReq.getColor().trim() : "";
        if (raw.isEmpty()) {
            throw new IllegalArgumentException("Color is required.");
        }
        if (raw.matches("\\d+")) {
            Long id = Long.parseLong(raw);
            colorRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Color not found: " + raw));
            return String.valueOf(id);
        }
        return colorRepository.findAllByOrderByColorNameAsc().stream()
                .filter(c -> raw.equalsIgnoreCase(c.getColorName()))
                .findFirst()
                .map(c -> String.valueOf(c.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Color not found: " + raw));
    }

    private String resolveSizeId(CreateProductVariantRequest variantReq) {
        if (variantReq.getSizeId() != null) {
            return String.valueOf(variantReq.getSizeId());
        }
        String raw = variantReq.getSize() != null ? variantReq.getSize().trim() : "";
        if (raw.isEmpty()) {
            throw new IllegalArgumentException("Size is required.");
        }
        if (raw.matches("\\d+")) {
            Long id = Long.parseLong(raw);
            sizeRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Size not found: " + raw));
            return String.valueOf(id);
        }
        String normalized = raw.replaceAll("\\s*\\(.*\\)$", "").trim();
        return sizeRepository.findAllByOrderBySizeNameAsc().stream()
                .filter(s -> raw.equalsIgnoreCase(s.getSizeName())
                        || raw.equalsIgnoreCase(s.getSizeCode())
                        || normalized.equalsIgnoreCase(s.getSizeName())
                        || (s.getSizeName() + " (" + s.getSizeCode() + ")").equalsIgnoreCase(raw))
                .findFirst()
                .map(s -> String.valueOf(s.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Size not found: " + raw));
    }

    private String generateSku(String name) {
        String base = name.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (base.length() > 20) {
            base = base.substring(0, 20);
        }
        return "ADM-" + base + "-" + System.currentTimeMillis() % 100000;
    }

    private String generateVariantSku(String productName, String color, String size) {
        String base = (productName + "-" + color + "-" + size)
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (base.length() > 40) {
            base = base.substring(0, 40);
        }
        return base + "-" + (System.currentTimeMillis() % 10000);
    }

    private Map<String, Object> variantRef(String clientKey, Long variantId) {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("clientKey", clientKey);
        ref.put("variantId", variantId);
        return ref;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private record ResolvedCatalog(Integer categoryId, Integer subcategoryId, Subcategory subcategory) {}

    private record DeliveryCharges(BigDecimal intraCity, BigDecimal metroMetro) {}
}

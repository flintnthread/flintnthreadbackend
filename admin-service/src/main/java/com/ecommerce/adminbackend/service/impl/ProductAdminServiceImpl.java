package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.entity.Color;
import com.ecommerce.adminbackend.entity.Product;
import com.ecommerce.adminbackend.entity.ProductImage;
import com.ecommerce.adminbackend.entity.ProductVariant;
import com.ecommerce.adminbackend.entity.Seller;
import com.ecommerce.adminbackend.entity.Category;
import com.ecommerce.adminbackend.entity.Size;
import com.ecommerce.adminbackend.entity.SizeChart;
import com.ecommerce.adminbackend.entity.Subcategory;
import com.ecommerce.adminbackend.repository.CategoryRepository;
import com.ecommerce.adminbackend.repository.ColorRepository;
import com.ecommerce.adminbackend.repository.ProductImageRepository;
import com.ecommerce.adminbackend.repository.ProductRepository;
import com.ecommerce.adminbackend.repository.ProductVariantRepository;
import com.ecommerce.adminbackend.repository.SellerRepository;
import com.ecommerce.adminbackend.repository.SizeChartRepository;
import com.ecommerce.adminbackend.repository.SizeRepository;
import com.ecommerce.adminbackend.repository.SubcategoryRepository;
import com.ecommerce.adminbackend.dto.product.CreateProductRequest;
import com.ecommerce.adminbackend.dto.product.UpdateProductRequest;
import com.ecommerce.adminbackend.service.ProductAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import com.ecommerce.adminbackend.service.support.ProductVariantCommissionSupport;
import com.ecommerce.adminbackend.util.MediaUrlHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductAdminServiceImpl extends BaseAdminService implements ProductAdminService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final SellerRepository sellerRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final ColorRepository colorRepository;
    private final SizeRepository sizeRepository;
    private final SizeChartRepository sizeChartRepository;
    private final MediaUrlHelper mediaUrlHelper;
    private final ObjectMapper objectMapper;
    private final ProductVariantCommissionSupport commissionSupport;
    private final ProductAdminMutationService productAdminMutationService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listPending(int page, int size) {
        var result = productRepository.findByStatusIgnoreCase("pending", PageRequest.of(page, size));
        return PageResponse.from(result.map(this::toProductSummary));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listProducts(
            String status,
            String search,
            Long sellerId,
            Boolean adminOnly,
            Integer mainCategoryId,
            Integer categoryId,
            Integer subcategoryId,
            int page,
            int size) {
        String normalizedStatus = normalizeListStatus(status);
        Page<Product> result = productRepository.searchProducts(
                normalizedStatus,
                blankToNull(search),
                Boolean.TRUE.equals(adminOnly) ? null : sellerId,
                adminOnly,
                mainCategoryId,
                categoryId,
                subcategoryId,
                PageRequest.of(page, size));
        return PageResponse.from(result.map(this::toProductListRow));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        long active = productRepository.countApproved();
        long pending = productRepository.countByStatusIgnoreCase("pending");
        long rejected = productRepository.countRejected();
        long underReview = productRepository.countByStatusIgnoreCase("under_review");
        long total = productRepository.count();
        stats.put("pending", pending);
        stats.put("approved", active);
        stats.put("active", active);
        stats.put("rejected", rejected);
        stats.put("underReview", underReview);
        stats.put("inactive", productRepository.countInactiveProducts());
        stats.put("total", total);
        stats.put("outOfStock", countOutOfStock());
        stats.put("lowStock", countLowStock());
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> catalog() {
        List<Map<String, Object>> categories = new ArrayList<>();
        for (Category category : categoryRepository.findAll()) {
            List<Map<String, Object>> subcategories = subcategoryRepository
                    .findByCategoryIdOrderBySubcategoryNameAsc(category.getId())
                    .stream()
                    .map(sub -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("id", sub.getId());
                        row.put("name", sub.getSubcategoryName());
                        return row;
                    })
                    .toList();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", category.getId());
            row.put("name", category.getCategoryName());
            row.put("subcategories", subcategories);
            categories.add(row);
        }

        List<Map<String, Object>> colors = colorRepository.findAllByOrderByColorNameAsc().stream()
                .filter(c -> Boolean.TRUE.equals(c.getStatus()))
                .map(c -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", c.getId());
                    row.put("name", c.getColorName());
                    row.put("code", c.getColorCode());
                    return row;
                })
                .toList();

        List<Map<String, Object>> sizes = sizeRepository.findAllByOrderBySizeNameAsc().stream()
                .filter(s -> Boolean.TRUE.equals(s.getStatus()))
                .map(s -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", s.getId());
                    row.put("name", s.getSizeName());
                    row.put("code", s.getSizeCode());
                    return row;
                })
                .toList();

        Map<String, Object> catalog = new LinkedHashMap<>();
        catalog.put("categories", categories);
        catalog.put("colors", colors);
        catalog.put("sizes", sizes);
        return catalog;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getProduct(Long id) {
        Product product = requireProduct(id);
        List<ProductVariant> variants = productVariantRepository.findByProductIdOrderByIdAsc(id);
        List<ProductImage> images = productImageRepository.findByProductIdOrderBySortOrderAsc(id);
        Seller seller = product.getSellerId() != null
                ? sellerRepository.findById(product.getSellerId()).orElse(null)
                : null;
        Map<Long, Color> colorById = loadColors(variants);
        Map<Long, Size> sizeById = loadSizes(variants);
        String fallbackImage = resolveListProductImage(images, variants.isEmpty() ? null : variants.get(0));

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", product.getId());
        detail.put("name", product.getName());
        detail.put("sku", resolveProductSku(product, variants));
        detail.put("status", product.getStatus());
        detail.put("categoryId", product.getCategoryId());
        detail.put("subcategoryId", product.getSubcategoryId());
        detail.put("sellerId", product.getSellerId());
        detail.put("addedByAdmin", product.getSellerId() == null);
        detail.put("sellerName", seller != null ? seller.getFullName() : "Admin Catalog");
        detail.put("sellerEmail", seller != null ? seller.getEmail() : null);
        detail.put("sellerPhone", seller != null ? seller.getMobile() : null);
        detail.put("mainCategoryName", resolveMainCategoryName(product.getCategoryId()));
        detail.put("shortDescription", product.getShortDescription());
        detail.put("description", product.getDescription());
        detail.put("features", product.getFeatures());
        detail.put("specifications", product.getSpecifications());
        detail.put("gstPercentage", product.getGstPercentage());
        detail.put("adminNotes", product.getAdminNotes());
        detail.put("createdAt", product.getCreatedAt());
        detail.put("updatedAt", product.getUpdatedAt());
        detail.put("reviewedAt", product.getReviewedAt());
        detail.put("hsnCode", product.getHsnCode());
        detail.put("productWeight", product.getProductWeight());
        detail.put("returnPolicy", product.getReturnPolicy());
        detail.put("deliveryTimeMin", product.getDeliveryTimeMin());
        detail.put("deliveryTimeMax", product.getDeliveryTimeMax());
        detail.put("deliveryInfo", product.getDeliveryInfo());
        detail.put("warrantyInfo", product.getWarrantyInfo());
        detail.put("careInstructions", product.getCareInstructions());
        detail.put("productMaterialType", product.getProductMaterialType());
        detail.put("productWeight", product.getProductWeight());
        detail.put("lengthCm", product.getLengthCm());
        detail.put("widthCm", product.getWidthCm());
        detail.put("heightCm", product.getHeightCm());
        detail.put("fragile", product.getFragile());
        detail.put("intraCityCharge", product.getIntraCityCharge());
        detail.put("metroMetroCharge", product.getMetroMetroCharge());
        detail.put("acceptCod", product.getAcceptCod());
        detail.put("deliverAllLocations", product.getDeliverAllLocations());
        detail.put("sizeChartId", product.getSizeChartId());
        detail.put("categoryName", resolveCategoryName(product.getCategoryId()));
        detail.put("subcategoryName", resolveSubcategoryName(product.getSubcategoryId()));
        detail.put("variants", variants.stream()
                .map(variant -> toVariant(
                        variant,
                        images,
                        product.getGstPercentage(),
                        colorById,
                        sizeById,
                        fallbackImage,
                        seller))
                .toList());
        detail.put("images", images.stream().map(this::toImage).toList());
        if (product.getSizeChartId() != null) {
            sizeChartRepository.findById(product.getSizeChartId()).ifPresent(chart -> {
                detail.put("sizeChartName", chart.getChartName());
                detail.put("sizeChartImage", mediaUrlHelper.toPublicUrl(chart.getChartImage()));
                detail.put("sizeChartRows", parseSizeChartRows(chart.getChartData()));
            });
        }
        return detail;
    }

    @Override
    @Transactional
    public Map<String, Object> create(CreateProductRequest request) {
        return productAdminMutationService.create(request);
    }

    @Override
    @Transactional
    public Map<String, Object> update(Long id, UpdateProductRequest request) {
        return productAdminMutationService.update(id, request);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        productAdminMutationService.delete(id);
    }

    @Override
    @Transactional
    public Map<String, Object> approve(Long id, String note) {
        Product product = requireProduct(id);
        product.setStatus("active");
        product.setReviewedAt(LocalDateTime.now());
        if (note != null && !note.isBlank()) {
            product.setAdminNotes(note.trim());
        }
        applyCommissionToVariants(product);
        productRepository.save(product);
        log.info("Product approved: id={}", id);
        return Map.of("productId", id, "status", "approved", "message", "Product approved.");
    }

    @Override
    @Transactional
    public Map<String, Object> reject(Long id, String note) {
        Product product = requireProduct(id);
        product.setStatus("rejected");
        product.setReviewedAt(LocalDateTime.now());
        product.setAdminNotes(note != null && !note.isBlank() ? note.trim() : "Product rejected.");
        productRepository.save(product);
        log.info("Product rejected: id={}", id);
        return Map.of("productId", id, "status", "rejected", "message", "Product rejected.");
    }

    private Product requireProduct(Long id) {
        return requireFound(productRepository.findById(id), "Product not found.");
    }

    private Map<String, Object> toProductSummary(Product product) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", product.getId());
        summary.put("name", product.getName());
        summary.put("sku", product.getSku());
        summary.put("status", product.getStatus());
        summary.put("sellerId", product.getSellerId());
        summary.put("createdAt", product.getCreatedAt());
        return summary;
    }

    private Map<String, Object> toProductListRow(Product product) {
        List<ProductVariant> variants = productVariantRepository.findByProductIdOrderByIdAsc(product.getId());
        List<ProductImage> images = productImageRepository.findByProductIdOrderBySortOrderAsc(product.getId());
        int totalStock = variants.stream().mapToInt(v -> v.getStock() != null ? v.getStock() : 0).sum();
        ProductVariant firstVariant = variants.isEmpty() ? null : variants.get(0);
        Seller listSeller = product.getSellerId() != null
                ? sellerRepository.findById(product.getSellerId()).orElse(null)
                : null;
        BigDecimal commissionPercent = commissionSupport.resolveCommissionPercent(listSeller);
        BigDecimal price = firstVariant != null
                ? commissionSupport.enrich(firstVariant, commissionPercent, product.getGstPercentage()).displayPrice()
                : BigDecimal.ZERO;
        String imageUrl = resolveListProductImage(images, firstVariant);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", product.getId());
        row.put("name", product.getName());
        row.put("sku", resolveProductSku(product, firstVariant));
        row.put("status", product.getStatus());
        row.put("sellerId", product.getSellerId());
        row.put("addedByAdmin", product.getSellerId() == null);
        row.put("sellerName", listSeller != null ? listSeller.getFullName() : "Admin Catalog");
        row.put("sellerEmail", listSeller != null ? listSeller.getEmail() : null);
        row.put("categoryId", product.getCategoryId());
        row.put("subcategoryId", product.getSubcategoryId());
        row.put("categoryName", resolveCategoryName(product.getCategoryId()));
        row.put("mainCategoryName", resolveMainCategoryName(product.getCategoryId()));
        row.put("subcategoryName", resolveSubcategoryName(product.getSubcategoryId()));
        row.put("color", firstVariant != null ? firstVariant.getColor() : null);
        row.put("size", firstVariant != null ? firstVariant.getSize() : null);
        row.put("price", price);
        row.put("stock", totalStock);
        row.put("displayStatus", resolveDisplayStatus(product.getStatus(), totalStock));
        row.put("imageUrl", imageUrl);
        row.put("createdAt", product.getCreatedAt());
        row.put("updatedAt", product.getUpdatedAt());
        row.put("updatedLabel", formatRelativeTime(product.getUpdatedAt() != null
                ? product.getUpdatedAt() : product.getCreatedAt()));
        return row;
    }

    private String normalizeListStatus(String status) {
        if (status == null || status.isBlank() || "all".equalsIgnoreCase(status.trim())) {
            return null;
        }
        String normalized = status.trim().toLowerCase();
        if ("active".equals(normalized) || "approved".equals(normalized)) {
            return "active";
        }
        return normalized;
    }

    private String resolveDisplayStatus(String status, int stock) {
        if (stock <= 0) {
            return "Out of Stock";
        }
        String normalized = status != null ? status.toLowerCase() : "";
        if ("active".equals(normalized) || "approved".equals(normalized)) {
            return "Active";
        }
        if ("rejected".equals(normalized) || "inactive".equals(normalized)) {
            return "Inactive";
        }
        return "Inactive";
    }

    private String resolveCategoryName(Integer categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .map(Category::getCategoryName)
                .orElse(null);
    }

    private String resolveMainCategoryName(Integer categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .map(category -> {
                    if (category.getParentId() == null) {
                        return category.getCategoryName();
                    }
                    return categoryRepository.findById(category.getParentId())
                            .map(Category::getCategoryName)
                            .orElse(category.getCategoryName());
                })
                .orElse(null);
    }

    private String resolveSubcategoryName(Integer subcategoryId) {
        if (subcategoryId == null) {
            return null;
        }
        return subcategoryRepository.findById(subcategoryId)
                .map(Subcategory::getSubcategoryName)
                .orElse(null);
    }

    private String formatRelativeTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "—";
        }
        long days = ChronoUnit.DAYS.between(dateTime.toLocalDate(), LocalDateTime.now().toLocalDate());
        if (days <= 0) {
            return "Today";
        }
        if (days == 1) {
            return "1 day ago";
        }
        if (days < 7) {
            return days + " days ago";
        }
        if (days < 30) {
            return (days / 7) + " week" + (days / 7 > 1 ? "s" : "") + " ago";
        }
        return DateTimeFormatter.ofPattern("dd MMM, yyyy").format(dateTime);
    }

    private long countOutOfStock() {
        return productVariantRepository.countOutOfStockProducts();
    }

    private long countLowStock() {
        return productVariantRepository.countLowStockProducts();
    }

    private void applyCommissionToVariants(Product product) {
        Seller seller = product.getSellerId() != null
                ? sellerRepository.findById(product.getSellerId()).orElse(null)
                : null;
        BigDecimal commissionPercent = commissionSupport.resolveCommissionPercent(seller);
        for (ProductVariant variant : productVariantRepository.findByProductIdOrderByIdAsc(product.getId())) {
            commissionSupport.applyCommission(variant, commissionPercent, product.getGstPercentage(), true);
            productVariantRepository.save(variant);
        }
    }

    private Map<String, Object> toVariant(
            ProductVariant variant,
            List<ProductImage> images,
            BigDecimal defaultGst,
            Map<Long, Color> colorById,
            Map<Long, Size> sizeById,
            String fallbackImage,
            Seller seller) {
        String colorName = resolveColorName(variant.getColor(), colorById);
        String colorHex = resolveColorHex(variant.getColor(), colorById);
        String sizeName = resolveSizeName(variant.getSize(), sizeById);
        BigDecimal mrpExclGst = resolveMrpExclGst(variant);
        BigDecimal sellingExcl = firstDecimal(variant.getSellingPrice());
        BigDecimal sellingWith = firstDecimal(variant.getFinalPrice(), variant.getMrpPrice());
        BigDecimal taxPct = firstDecimal(variant.getTaxPercentage(), defaultGst);
        BigDecimal taxAmt = variant.getTaxAmount() != null ? variant.getTaxAmount() : BigDecimal.ZERO;
        if (taxAmt.compareTo(BigDecimal.ZERO) == 0 && sellingWith != null && sellingExcl != null) {
            taxAmt = sellingWith.subtract(sellingExcl).max(BigDecimal.ZERO);
        }
        BigDecimal commissionPercent = commissionSupport.resolveCommissionPercent(seller);
        ProductVariantCommissionSupport.EnrichedPricing pricing =
                commissionSupport.enrich(variant, commissionPercent, defaultGst);
        BigDecimal sellingWithGst = pricing.sellingPriceWithGst();
        BigDecimal commissionPct = pricing.commissionPercentage();
        BigDecimal commissionAmt = pricing.commissionAmount();
        BigDecimal intraCity = variant.getIntraCityDeliveryCharge() != null ? variant.getIntraCityDeliveryCharge() : BigDecimal.ZERO;
        BigDecimal metroMetro = variant.getMetroMetroDeliveryCharge() != null ? variant.getMetroMetroDeliveryCharge() : BigDecimal.ZERO;
        BigDecimal discountPct = variant.getDiscountPercentage() != null
                ? variant.getDiscountPercentage()
                : resolveDiscountPercent(variant, sellingWith, mrpExclGst);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", variant.getId());
        row.put("color", colorName);
        row.put("colorId", parseCatalogId(variant.getColor()).orElse(null));
        row.put("colorHex", colorHex);
        row.put("size", sizeName);
        row.put("sizeId", parseCatalogId(variant.getSize()).orElse(null));
        row.put("sku", variant.getSku());
        row.put("stock", variant.getStock());
        row.put("basePrice", variant.getBasePrice());
        row.put("mrpExclGst", mrpExclGst);
        row.put("mrpInclGst", variant.getMrpInclGst());
        row.put("mrpPrice", variant.getMrpPrice());
        row.put("discountPercentage", discountPct);
        row.put("discountAmount", variant.getDiscountAmount());
        row.put("sellingPrice", variant.getSellingPrice());
        row.put("taxPercentage", taxPct);
        row.put("taxAmount", taxAmt);
        row.put("finalPrice", variant.getFinalPrice());
        row.put("sellingPriceWithGst", sellingWithGst);
        row.put("commissionPercentage", commissionPct);
        row.put("commissionAmount", commissionAmt);
        row.put("priceWithCommission", pricing.priceWithCommission());
        row.put("highestDeliveryCharge", pricing.highestDeliveryCharge());
        row.put("displayPrice", pricing.displayPrice());
        row.put("intraCityDeliveryCharge", intraCity);
        row.put("metroMetroDeliveryCharge", metroMetro);
        row.put("totalPriceIntraCity", pricing.totalPriceIntraCity());
        row.put("totalPriceMetroMetro", pricing.totalPriceMetroMetro());
        row.put("customerPrice", pricing.displayPrice());
        row.put("imageUrl", resolveVariantImage(images, variant.getId(), fallbackImage));
        row.put("weight", variant.getWeight());
        return row;
    }

    private Map<Long, Color> loadColors(List<ProductVariant> variants) {
        Set<Long> ids = variants.stream()
                .map(v -> parseCatalogId(v.getColor()))
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return colorRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Color::getId, color -> color));
    }

    private Map<Long, Size> loadSizes(List<ProductVariant> variants) {
        Set<Long> ids = variants.stream()
                .map(v -> parseCatalogId(v.getSize()))
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return sizeRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Size::getId, size -> size));
    }

    private Optional<Long> parseCatalogId(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(raw.trim()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private String resolveColorName(String raw, Map<Long, Color> colorById) {
        return parseCatalogId(raw)
                .map(colorById::get)
                .map(Color::getColorName)
                .orElse(raw != null && !raw.isBlank() ? raw : "—");
    }

    private String resolveColorHex(String raw, Map<Long, Color> colorById) {
        return parseCatalogId(raw)
                .map(colorById::get)
                .map(Color::getColorCode)
                .filter(code -> code != null && code.startsWith("#"))
                .orElse("#9CA3AF");
    }

    private String resolveSizeName(String raw, Map<Long, Size> sizeById) {
        return parseCatalogId(raw)
                .map(sizeById::get)
                .map(Size::getSizeName)
                .orElse(raw != null && !raw.isBlank() ? raw : "—");
    }

    private BigDecimal resolveMrpExclGst(ProductVariant variant) {
        if (variant.getMrpExclGst() != null) {
            return variant.getMrpExclGst();
        }
        if (variant.getBasePrice() != null) {
            return variant.getBasePrice();
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal resolveDiscountPercent(ProductVariant variant, BigDecimal sellingWith, BigDecimal mrpExcl) {
        if (mrpExcl == null || mrpExcl.compareTo(BigDecimal.ZERO) <= 0 || sellingWith == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount = mrpExcl.subtract(sellingWith)
                .multiply(BigDecimal.valueOf(100))
                .divide(mrpExcl, 2, RoundingMode.HALF_UP);
        return discount.max(BigDecimal.ZERO);
    }

    private BigDecimal firstDecimal(BigDecimal... values) {
        for (BigDecimal value : values) {
            if (value != null) {
                return value;
            }
        }
        return BigDecimal.ZERO;
    }

    private String resolveListProductImage(List<ProductImage> images, ProductVariant firstVariant) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        String primaryImage = images.stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .findFirst()
                .map(img -> toProductImageUrl(img.getImagePath()))
                .orElse(null);
        if (primaryImage != null && !primaryImage.isBlank()) {
            return primaryImage;
        }
        if (firstVariant != null) {
            String variantImage = resolveVariantImage(images, firstVariant.getId(), null);
            if (variantImage != null && !variantImage.isBlank()) {
                return variantImage;
            }
        }
        return images.stream()
                .findFirst()
                .map(img -> toProductImageUrl(img.getImagePath()))
                .orElse(null);
    }

    private String resolveProductSku(Product product, ProductVariant firstVariant) {
        if (product != null && product.getSku() != null && !product.getSku().isBlank()) {
            return product.getSku().trim();
        }
        if (firstVariant != null && firstVariant.getSku() != null && !firstVariant.getSku().isBlank()) {
            return firstVariant.getSku().trim();
        }
        return null;
    }

    private String resolveProductSku(Product product, List<ProductVariant> variants) {
        ProductVariant firstVariant = variants == null || variants.isEmpty() ? null : variants.get(0);
        String sku = resolveProductSku(product, firstVariant);
        if (sku != null && !sku.isBlank()) {
            return sku;
        }
        if (variants == null) {
            return null;
        }
        for (ProductVariant variant : variants) {
            if (variant.getSku() != null && !variant.getSku().isBlank()) {
                return variant.getSku().trim();
            }
        }
        return null;
    }

    private String toProductImageUrl(String path) {
        return mediaUrlHelper.toPublicUrl(path, "products");
    }

    private String resolveVariantImage(List<ProductImage> images, Long variantId, String fallbackImage) {
        String variantImage = images.stream()
                .filter(img -> Objects.equals(variantId, img.getVariantId()))
                .findFirst()
                .map(img -> toProductImageUrl(img.getImagePath()))
                .orElse(null);
        if (variantImage != null && !variantImage.isBlank()) {
            return variantImage;
        }
        return fallbackImage != null ? fallbackImage : "";
    }

    private List<Map<String, Object>> parseSizeChartRows(String chartData) {
        if (chartData == null || chartData.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(chartData);
            JsonNode measurements = root.path("measurements");
            if (measurements.isArray() && !measurements.isEmpty() && measurements.get(0).has("size_name")) {
                List<Map<String, Object>> rows = new ArrayList<>();
                for (JsonNode row : measurements) {
                    rows.add(sizeChartRow(
                            row.path("size_name").asText("—"),
                            row.path("chest_bust").asText("—"),
                            row.path("waist").asText("—"),
                            row.path("hip").asText("—"),
                            row.path("length").asText("—")));
                }
                return rows;
            }
            JsonNode sizes = root.path("sizes");
            if (sizes.isArray()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                for (JsonNode row : sizes) {
                    rows.add(sizeChartRow(
                            row.path("size").asText("—"),
                            row.path("chest").asText("—"),
                            row.path("waist").asText("—"),
                            row.path("hip").asText("—"),
                            row.path("length").asText("—")));
                }
                return rows;
            }
        } catch (Exception ignored) {
            // fall through
        }
        return List.of();
    }

    private Map<String, Object> sizeChartRow(String size, String chest, String waist, String hip, String length) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("size", size);
        row.put("chest", chest);
        row.put("waist", waist);
        row.put("hip", hip);
        row.put("length", length);
        return row;
    }

    private Map<String, Object> toImage(ProductImage image) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", image.getId());
        row.put("url", toProductImageUrl(image.getImagePath()));
        row.put("isPrimary", image.getIsPrimary());
        row.put("sortOrder", image.getSortOrder());
        return row;
    }
}

package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.dto.BulkImportResponse;
import com.ecommerce.sellerbackend.dto.CreateProductRequest;
import com.ecommerce.sellerbackend.dto.CreateProductResponse;
import com.ecommerce.sellerbackend.dto.CatalogCategoryResponse;
import com.ecommerce.sellerbackend.dto.ProductDeliverySettingsResponse;
import com.ecommerce.sellerbackend.dto.ProductDetailResponse;
import com.ecommerce.sellerbackend.dto.ProductListItemResponse;
import com.ecommerce.sellerbackend.dto.UpdateProductDeliveryRequest;
import com.ecommerce.sellerbackend.dto.UpdateProductRequest;
import com.ecommerce.sellerbackend.dto.VariantMutationRequest;
import org.springframework.web.multipart.MultipartFile;
import com.ecommerce.sellerbackend.dto.ProductVariantSummaryResponse;
import com.ecommerce.sellerbackend.entity.Category;
import com.ecommerce.sellerbackend.entity.Color;
import com.ecommerce.sellerbackend.entity.Product;
import com.ecommerce.sellerbackend.entity.ProductImage;
import com.ecommerce.sellerbackend.entity.ProductVariant;
import com.ecommerce.sellerbackend.entity.Size;
import com.ecommerce.sellerbackend.entity.SizeChart;
import com.ecommerce.sellerbackend.entity.Subcategory;
import com.ecommerce.sellerbackend.exception.ResourceNotFoundException;
import com.ecommerce.sellerbackend.repository.CategoryRepository;
import com.ecommerce.sellerbackend.repository.ColorRepository;
import com.ecommerce.sellerbackend.repository.ProductImageRepository;
import com.ecommerce.sellerbackend.repository.ProductRepository;
import com.ecommerce.sellerbackend.repository.ProductVariantRepository;
import com.ecommerce.sellerbackend.repository.SizeChartRepository;
import com.ecommerce.sellerbackend.repository.SizeRepository;
import com.ecommerce.sellerbackend.repository.SubcategoryRepository;
import com.ecommerce.sellerbackend.service.CatalogHierarchyService;
import com.ecommerce.sellerbackend.service.ProductDetailAssembler;
import com.ecommerce.sellerbackend.service.ProductService;
import com.ecommerce.sellerbackend.service.support.ProductSpecificationsCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final ColorRepository colorRepository;
    private final SizeRepository sizeRepository;
    private final SizeChartRepository sizeChartRepository;
    private final ProductDetailAssembler productDetailAssembler;
    private final ProductCreateService productCreateService;
    private final ProductUpdateService productUpdateService;
    private final ProductDeleteService productDeleteService;
    private final ProductVariantMutationService productVariantMutationService;
    private final ProductBulkImportService productBulkImportService;
    private final ProductDeliveryService productDeliveryService;
    private final CatalogHierarchyService catalogHierarchyService;

    @Value("${app.media.public-base-url:}")
    private String mediaBaseUrl;

    @Override
    @Transactional(readOnly = true)
    public List<ProductListItemResponse> listForSeller(Long sellerId) {
        List<Product> products = productRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
        if (products.isEmpty()) {
            return List.of();
        }

        List<Long> productIds = products.stream().map(Product::getId).toList();
        Map<Long, List<ProductVariant>> variantsByProduct = productVariantRepository
                .findByProductIdIn(productIds)
                .stream()
                .collect(Collectors.groupingBy(ProductVariant::getProductId));

        Map<Long, List<ProductImage>> imagesByProduct = productImageRepository
                .findByProductIdInOrderByIsPrimaryDescSortOrderAsc(productIds)
                .stream()
                .collect(Collectors.groupingBy(ProductImage::getProductId));

        Set<Integer> categoryIds = products.stream().map(Product::getCategoryId).collect(Collectors.toSet());
        Set<Integer> subcategoryIds = products.stream().map(Product::getSubcategoryId).collect(Collectors.toSet());
        Map<Integer, String> categoryNames = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getCategoryName));
        Map<Integer, String> subcategoryNames = subcategoryRepository.findAllById(subcategoryIds).stream()
                .collect(Collectors.toMap(Subcategory::getId, Subcategory::getSubcategoryName));
        List<CatalogCategoryResponse> categoryTree = catalogHierarchyService.loadCategoryTree();

        Set<Long> colorIds = new HashSet<>();
        Set<Long> sizeIds = new HashSet<>();
        for (List<ProductVariant> variantList : variantsByProduct.values()) {
            for (ProductVariant variant : variantList) {
                parseCatalogId(variant.getColor()).ifPresent(colorIds::add);
                parseCatalogId(variant.getSize()).ifPresent(sizeIds::add);
            }
        }
        Map<Long, Color> colorById = colorRepository.findAllById(colorIds).stream()
                .collect(Collectors.toMap(Color::getId, Function.identity()));
        Map<Long, Size> sizeById = sizeRepository.findAllById(sizeIds).stream()
                .collect(Collectors.toMap(Size::getId, Function.identity()));

        List<ProductListItemResponse> result = new ArrayList<>();
        for (Product product : products) {
            List<ProductVariant> variants = variantsByProduct.getOrDefault(product.getId(), List.of());
            List<ProductImage> images = imagesByProduct.getOrDefault(product.getId(), List.of());
            result.add(toListItem(
                    product,
                    variants,
                    images,
                    categoryTree,
                    categoryNames.getOrDefault(product.getCategoryId(), "Uncategorized"),
                    subcategoryNames.getOrDefault(product.getSubcategoryId(), ""),
                    colorById,
                    sizeById));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getDetailForSeller(Long sellerId, Long productId) {
        Product product = productRepository.findByIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));

        List<ProductVariant> variants = productVariantRepository.findByProductIdOrderByIdAsc(productId);
        List<ProductImage> images = productImageRepository
                .findByProductIdInOrderByIsPrimaryDescSortOrderAsc(List.of(productId));

        Set<Long> colorIds = new HashSet<>();
        Set<Long> sizeIds = new HashSet<>();
        for (ProductVariant variant : variants) {
            parseCatalogId(variant.getColor()).ifPresent(colorIds::add);
            parseCatalogId(variant.getSize()).ifPresent(sizeIds::add);
        }

        Map<Long, Color> colorById = colorRepository.findAllById(colorIds).stream()
                .collect(Collectors.toMap(Color::getId, Function.identity()));
        Map<Long, Size> sizeById = sizeRepository.findAllById(sizeIds).stream()
                .collect(Collectors.toMap(Size::getId, Function.identity()));

        String fallbackCategory = categoryRepository.findById(product.getCategoryId())
                .map(Category::getCategoryName)
                .orElse("Category " + product.getCategoryId());
        String leafFromSpecs = ProductSpecificationsCodec.parseLeafSubcategoryName(product.getSpecifications());
        var categoryPath = catalogHierarchyService.resolveCategoryPath(
                product.getCategoryId(),
                product.getSubcategoryId(),
                leafFromSpecs);
        String categoryName = categoryPath.mainCategory().isBlank() ? fallbackCategory : categoryPath.mainCategory();
        String categorySubName = categoryPath.middleCategory();
        String subcategoryName = !categoryPath.leafSubcategory().isBlank()
                ? categoryPath.leafSubcategory()
                : categoryPath.middleCategory();

        SizeChart sizeChart = product.getSizeChartId() != null
                ? sizeChartRepository.findById(product.getSizeChartId()).orElse(null)
                : null;

        return productDetailAssembler.assemble(
                product,
                categoryName,
                categorySubName,
                subcategoryName,
                variants,
                images,
                colorById,
                sizeById,
                sizeChart);
    }

    @Override
    @Transactional
    public CreateProductResponse createForSeller(Long sellerId, CreateProductRequest request) {
        return productCreateService.create(sellerId, request);
    }

    @Override
    @Transactional
    public CreateProductResponse updateForSeller(Long sellerId, Long productId, UpdateProductRequest request) {
        return productUpdateService.update(sellerId, productId, request);
    }

    @Override
    @Transactional
    public void deleteForSeller(Long sellerId, Long productId) {
        productDeleteService.deleteForSeller(sellerId, productId);
    }

    @Override
    @Transactional
    public CreateProductResponse.CreatedVariantRef createVariant(
            Long sellerId, Long productId, VariantMutationRequest request) {
        return productVariantMutationService.createVariant(sellerId, productId, request);
    }

    @Override
    @Transactional
    public CreateProductResponse.CreatedVariantRef updateVariant(
            Long sellerId, Long productId, Long variantId, VariantMutationRequest request) {
        return productVariantMutationService.updateVariant(sellerId, productId, variantId, request);
    }

    @Override
    @Transactional
    public void deleteVariant(Long sellerId, Long productId, Long variantId) {
        productVariantMutationService.deleteVariant(sellerId, productId, variantId);
    }

    @Override
    @Transactional
    public BulkImportResponse bulkImport(Long sellerId, MultipartFile file) {
        return productBulkImportService.importZip(sellerId, file);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDeliverySettingsResponse getDeliverySettings(Long sellerId, Long productId, String search) {
        return productDeliveryService.getSettings(sellerId, productId, search);
    }

    @Override
    @Transactional
    public ProductDeliverySettingsResponse updateDeliverySettings(
            Long sellerId, Long productId, UpdateProductDeliveryRequest request) {
        return productDeliveryService.updateSettings(sellerId, productId, request);
    }

    private java.util.Optional<Long> parseCatalogId(String raw) {
        if (raw == null || raw.isBlank()) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(Long.parseLong(raw.trim()));
        } catch (NumberFormatException ex) {
            return java.util.Optional.empty();
        }
    }

    private ProductListItemResponse toListItem(
            Product product,
            List<ProductVariant> variants,
            List<ProductImage> images,
            List<CatalogCategoryResponse> categoryTree,
            String categoryName,
            String subcategoryName,
            Map<Long, Color> colorById,
            Map<Long, Size> sizeById) {
        var categoryPath = catalogHierarchyService.resolveCategoryPath(
                categoryTree,
                product.getCategoryId(),
                product.getSubcategoryId(),
                ProductSpecificationsCodec.parseLeafSubcategoryName(product.getSpecifications()));
        String displayMain = categoryPath.mainCategory().isBlank() ? categoryName : categoryPath.mainCategory();
        String displayMiddle = categoryPath.middleCategory().isBlank() ? subcategoryName : categoryPath.middleCategory();
        String displayLeaf = categoryPath.leafSubcategory();
        String displaySubcategory = !displayLeaf.isBlank() ? displayLeaf : displayMiddle;
        int totalStock = variants.stream()
                .map(ProductVariant::getStock)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        ProductVariant displayVariant = pickMinimumPriceVariant(variants);
        BigDecimal price = resolvePrice(displayVariant);
        BigDecimal mrpInclGst = displayVariant != null && displayVariant.getMrpInclGst() != null
                ? displayVariant.getMrpInclGst()
                : BigDecimal.ZERO;
        String image = resolveProductImage(images, displayVariant);
        String color = resolveCatalogLabel(displayVariant != null ? displayVariant.getColor() : null, colorById, Color::getColorName);
        String size = resolveCatalogLabel(displayVariant != null ? displayVariant.getSize() : null, sizeById, Size::getSizeName);
        Integer minQuantity = displayVariant != null ? displayVariant.getMinQuantity() : null;

        List<ProductVariantSummaryResponse> variantSummaries = variants.stream()
                .sorted(Comparator.comparing(ProductVariant::getId))
                .map(v -> ProductVariantSummaryResponse.builder()
                        .id(v.getId())
                        .sku(v.getSku())
                        .color(resolveCatalogLabel(v.getColor(), colorById, Color::getColorName))
                        .size(resolveCatalogLabel(v.getSize(), sizeById, Size::getSizeName))
                        .stock(v.getStock())
                        .minQuantity(v.getMinQuantity())
                        .sellingPrice(v.getSellingPrice())
                        .finalPrice(v.getFinalPrice())
                        .metroMetroDeliveryCharge(v.getMetroMetroDeliveryCharge())
                        .image(resolveVariantImage(images, v.getId()))
                        .build())
                .toList();

        return ProductListItemResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(firstNonBlank(
                        displayVariant != null ? displayVariant.getSku() : null,
                        product.getSku()))
                .price(price)
                .mrpInclGst(mrpInclGst)
                .image(image)
                .status(resolveDisplayStatus(product.getStatus(), totalStock))
                .stock(totalStock)
                .updated(formatDate(product.getUpdatedAt() != null
                        ? product.getUpdatedAt()
                        : product.getCreatedAt()))
                .categoryId(product.getCategoryId())
                .category(displayMain)
                .categorySub(displayMiddle)
                .subcategoryId(product.getSubcategoryId())
                .subcategory(displaySubcategory)
                .color(color)
                .size(size)
                .minQuantity(minQuantity)
                .description(firstNonBlank(product.getShortDescription(), product.getDescription()))
                .material(product.getProductMaterialType())
                .weight(formatWeight(product.getProductWeight()))
                .dimensions(formatDimensions(product))
                .returnPolicy(product.getReturnPolicy())
                .warranty(product.getWarrantyInfo())
                .variants(variantSummaries)
                .build();
    }

    private ProductVariant pickMinimumPriceVariant(List<ProductVariant> variants) {
        if (variants.isEmpty()) {
            return null;
        }
        return variants.stream()
                .min(Comparator
                        .comparing(this::resolvePrice, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ProductVariant::getId))
                .orElse(variants.get(0));
    }

    private BigDecimal resolvePrice(ProductVariant variant) {
        if (variant == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal sellingWithGst = variant.getFinalPrice();
        if (sellingWithGst == null || sellingWithGst.compareTo(BigDecimal.ZERO) <= 0) {
            sellingWithGst = variant.getMrpPrice();
        }
        if (sellingWithGst != null && sellingWithGst.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal metroMetro = variant.getMetroMetroDeliveryCharge() != null
                    ? variant.getMetroMetroDeliveryCharge()
                    : BigDecimal.ZERO;
            return sellingWithGst.add(metroMetro).setScale(2, RoundingMode.HALF_UP);
        }
        if (variant.getSellingPrice() != null) {
            return variant.getSellingPrice();
        }
        if (variant.getBasePrice() != null) {
            return variant.getBasePrice();
        }
        return BigDecimal.ZERO;
    }

    private String resolveProductImage(List<ProductImage> images, ProductVariant primaryVariant) {
        if (images.isEmpty()) {
            return "";
        }
        ProductImage chosen = images.stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .findFirst()
                .orElse(images.get(0));

        if (primaryVariant != null) {
            chosen = images.stream()
                    .filter(img -> primaryVariant.getId().equals(img.getVariantId()))
                    .findFirst()
                    .orElse(chosen);
        }

        return resolveImageUrl(chosen.getImagePath());
    }

    private String resolveVariantImage(List<ProductImage> images, Long variantId) {
        return images.stream()
                .filter(img -> variantId.equals(img.getVariantId()))
                .findFirst()
                .map(img -> resolveImageUrl(img.getImagePath()))
                .orElse("");
    }

    private String resolveImageUrl(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            String lower = path.toLowerCase(Locale.ROOT);
            // New products: Cloudinary secure_url — return exactly as stored
            if (lower.contains("res.cloudinary.com/") || lower.contains("cloudinary.com/")) {
                return path;
            }
            // Old products: absolute URL that still points at /uploads/ — rewrite to media host
            int idx = path.indexOf("/uploads/");
            String base = mediaBaseUrl == null ? "" : mediaBaseUrl.trim();
            if (idx >= 0 && !base.isEmpty()) {
                if (base.endsWith("/")) {
                    base = base.substring(0, base.length() - 1);
                }
                return base + path.substring(idx);
            }
            return path;
        }
        // Old products: relative uploads/products/... path
        String base = mediaBaseUrl == null ? "" : mediaBaseUrl.trim();
        if (base.isEmpty()) {
            return path.startsWith("/") ? path : "/" + path;
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return path.startsWith("/") ? base + path : base + "/" + path;
    }

    public static String resolveDisplayStatus(String productStatus, int totalStock) {
        if (totalStock <= 0) {
            return "Out of Stock";
        }
        if (productStatus != null) {
            String normalized = productStatus.trim().toLowerCase(Locale.ROOT);
            if (normalized.equals("pending")) {
                return "Pending";
            }
            if (normalized.equals("inactive")
                    || normalized.equals("disabled")) {
                return "Deactivated";
            }
            if (normalized.equals("draft")
                    || normalized.equals("rejected")) {
                return "Inactive";
            }
        }
        return "Active";
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DISPLAY_DATE);
    }

    private String formatWeight(BigDecimal weight) {
        if (weight == null) {
            return "";
        }
        return weight.stripTrailingZeros().toPlainString() + " kg";
    }

    private String formatDimensions(Product product) {
        if (product.getLengthCm() == null
                && product.getWidthCm() == null
                && product.getHeightCm() == null) {
            return "";
        }
        return String.format(
                Locale.ENGLISH,
                "%s × %s × %s cm",
                valueOrDash(product.getLengthCm()),
                valueOrDash(product.getWidthCm()),
                valueOrDash(product.getHeightCm()));
    }

    private String valueOrDash(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private <T> String resolveCatalogLabel(
            String raw,
            Map<Long, T> catalogById,
            Function<T, String> labelFn) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return parseCatalogId(raw)
                .map(catalogById::get)
                .map(labelFn)
                .orElse(raw);
    }
}

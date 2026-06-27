package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.ProductDeliveryChargeResponse;
import com.ecommerce.sellerbackend.dto.ProductDeliveryInfoResponse;
import com.ecommerce.sellerbackend.dto.ProductDetailResponse;
import com.ecommerce.sellerbackend.dto.ProductPackagingResponse;
import com.ecommerce.sellerbackend.dto.ProductReturnDetailsResponse;
import com.ecommerce.sellerbackend.dto.ProductSizeChartRowResponse;
import com.ecommerce.sellerbackend.dto.ProductSpecResponse;
import com.ecommerce.sellerbackend.dto.ProductVariantDetailResponse;
import com.ecommerce.sellerbackend.entity.Color;
import com.ecommerce.sellerbackend.entity.Product;
import com.ecommerce.sellerbackend.entity.ProductImage;
import com.ecommerce.sellerbackend.entity.ProductVariant;
import com.ecommerce.sellerbackend.entity.Size;
import com.ecommerce.sellerbackend.entity.SizeChart;
import com.ecommerce.sellerbackend.service.impl.ProductServiceImpl;
import com.ecommerce.sellerbackend.service.support.ProductSpecificationsCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Component
public class ProductDetailAssembler {

    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.media.public-base-url:}")
    private String mediaBaseUrl;

    public ProductDetailResponse assemble(
            Product product,
            String categoryName,
            String categorySubName,
            String subcategoryName,
            List<ProductVariant> variants,
            List<ProductImage> images,
            Map<Long, Color> colorById,
            Map<Long, Size> sizeById,
            SizeChart sizeChart) {

        List<ProductVariantDetailResponse> variantDetails = variants.stream()
                .map(v -> toVariantDetail(v, images, colorById, sizeById))
                .toList();

        ProductVariant primary = pickPrimaryVariant(variants);
        int totalStock = variants.stream()
                .map(ProductVariant::getStock)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        BigDecimal price = resolveTotalMetroMetro(primary);
        BigDecimal mrpExclGst = resolveMrpExclGst(primary);
        BigDecimal mrpInclGst = primary != null ? primary.getMrpInclGst() : null;
        BigDecimal sellingExGst = primary != null ? primary.getSellingPrice() : null;
        int discount = primary != null && primary.getDiscountPercentage() != null
                ? primary.getDiscountPercentage().setScale(0, RoundingMode.HALF_UP).intValue()
                : resolveDiscountPercent(primary, price, mrpExclGst);

        String primaryColor = resolveColorName(primary != null ? primary.getColor() : null, colorById);
        String primarySize = resolveSizeName(primary != null ? primary.getSize() : null, sizeById);

        List<String> imageUrls = images.stream()
                .map(img -> resolveImageUrl(img.getImagePath()))
                .filter(url -> !url.isBlank())
                .toList();

        String deliveryEstimate = firstNonBlank(
                formatDeliveryEstimate(product.getDeliveryTimeMin(), product.getDeliveryTimeMax()),
                product.getDeliveryInfo(),
                "—");

        ProductSpecificationsCodec.CustomizationData customization =
                ProductSpecificationsCodec.parseCustomization(product.getSpecifications());

        return ProductDetailResponse.builder()
                .id(product.getId())
                .categoryId(product.getCategoryId())
                .subcategoryId(product.getSubcategoryId())
                .sizeChartId(product.getSizeChartId())
                .name(product.getName())
                .sku(firstNonBlank(product.getSku(), primary != null ? primary.getSku() : null, "—"))
                .price(price)
                .mrp(mrpExclGst)
                .mrpExclGst(mrpExclGst)
                .mrpInclGst(mrpInclGst)
                .sellingPriceExGst(sellingExGst)
                .discount(discount)
                .images(imageUrls)
                .status(ProductServiceImpl.resolveDisplayStatus(product.getStatus(), totalStock))
                .rawStatus(product.getStatus())
                .stock(totalStock)
                .updated(formatDate(product.getUpdatedAt() != null ? product.getUpdatedAt() : product.getCreatedAt()))
                .category(categoryName)
                .categorySub(categorySubName)
                .subcategory(subcategoryName)
                .color(primaryColor)
                .size(primarySize)
                .hsnCode(firstNonBlank(product.getHsnCode(), "—"))
                .gst(formatGst(product.getGstPercentage()))
                .createdAt(formatDate(product.getCreatedAt()))
                .approvedAt(formatDate(product.getReviewedAt()))
                .shortDescription(stripHtml(product.getShortDescription()))
                .description(stripHtml(firstNonBlank(product.getDescription(), product.getShortDescription(), "")))
                .material(firstNonBlank(product.getProductMaterialType(), "—"))
                .weight(formatWeight(product.getProductWeight()))
                .dimensions(formatDimensions(product))
                .returnPolicy(firstNonBlank(product.getReturnPolicy(), "—"))
                .warranty(firstNonBlank(product.getWarrantyInfo(), "No Warranty"))
                .careInstructions(firstNonBlank(product.getCareInstructions(), "—"))
                .adminNotes(firstNonBlank(product.getAdminNotes(), ""))
                .deliveryTimeMin(product.getDeliveryTimeMin())
                .deliveryTimeMax(product.getDeliveryTimeMax())
                .intraCityCharge(product.getIntraCityCharge())
                .metroMetroCharge(product.getMetroMetroCharge())
                .acceptCod(product.getAcceptCod())
                .fragile(product.getFragile())
                .customized(customization.isEnabled())
                .customTitle(customization.getTitle())
                .customInstructions(customization.getInstructions())
                .customLeadDays(customization.getLeadDays())
                .customCharge(customization.getCharge())
                .customAllowPhoto(customization.isAllowPhoto())
                .customImageLabel(customization.getImageLabel())
                .customAllowText(customization.isAllowText())
                .customTextLabel(customization.getTextLabel())
                .specifications(parseSpecifications(product.getSpecifications()))
                .features(parseFeatures(product.getFeatures()))
                .delivery(ProductDeliveryInfoResponse.builder()
                        .estimated(deliveryEstimate)
                        .freeAbove("—")
                        .expressAvailable(false)
                        .expressCharge("—")
                        .cod(Boolean.TRUE.equals(product.getAcceptCod()))
                        .codCharge("—")
                        .locations(Boolean.TRUE.equals(product.getDeliverAllLocations()) ? "Pan India" : "Selected locations")
                        .build())
                .packaging(ProductPackagingResponse.builder()
                        .boxDimensions(formatDimensions(product))
                        .grossWeight(formatWeight(product.getProductWeight()))
                        .packagingType(firstNonBlank(product.getProductMaterialType(), "Standard"))
                        .fragile(Boolean.TRUE.equals(product.getFragile()))
                        .build())
                .deliveryCharges(buildDeliveryCharges(product))
                .returnDetails(buildReturnDetails(product))
                .variants(variantDetails)
                .sizeChart(parseSizeChart(sizeChart))
                .sizeChartName(sizeChart != null ? sizeChart.getChartName() : null)
                .sizeChartImage(sizeChart != null ? resolveImageUrl(sizeChart.getChartImage()) : null)
                .build();
    }

    private ProductVariantDetailResponse toVariantDetail(
            ProductVariant variant,
            List<ProductImage> images,
            Map<Long, Color> colorById,
            Map<Long, Size> sizeById) {

        String colorName = resolveColorName(variant.getColor(), colorById);
        String colorHex = resolveColorHex(variant.getColor(), colorById);

        BigDecimal mrpExclGst = resolveMrpExclGst(variant);
        BigDecimal mrpInclGstVal = variant.getMrpInclGst();
        BigDecimal mrpPriceDb = variant.getMrpPrice();
        BigDecimal sellingPriceExGst = firstDecimal(variant.getSellingPrice());
        BigDecimal sellingPriceWithGst = firstDecimal(variant.getFinalPrice(), variant.getMrpPrice());
        BigDecimal mrp = mrpExclGst != null ? mrpExclGst : BigDecimal.ZERO;
        BigDecimal priceWithGst = sellingPriceWithGst != null ? sellingPriceWithGst : BigDecimal.ZERO;

        BigDecimal taxPct = variant.getTaxPercentage() != null ? variant.getTaxPercentage() : BigDecimal.ZERO;
        BigDecimal taxAmt = variant.getTaxAmount() != null ? variant.getTaxAmount() : BigDecimal.ZERO;
        BigDecimal commissionPct = variant.getCommissionPercentage() != null ? variant.getCommissionPercentage() : BigDecimal.ZERO;
        BigDecimal commissionAmt = variant.getCommissionAmount() != null ? variant.getCommissionAmount() : BigDecimal.ZERO;
        BigDecimal intraCity = variant.getIntraCityDeliveryCharge() != null ? variant.getIntraCityDeliveryCharge() : BigDecimal.ZERO;
        BigDecimal metroMetro = variant.getMetroMetroDeliveryCharge() != null ? variant.getMetroMetroDeliveryCharge() : BigDecimal.ZERO;
        BigDecimal totalIntra = variant.getTotalPriceIntraCity() != null ? variant.getTotalPriceIntraCity() : BigDecimal.ZERO;
        BigDecimal totalMetro = variant.getTotalPriceMetroMetro() != null ? variant.getTotalPriceMetroMetro() : BigDecimal.ZERO;

        int discount = variant.getDiscountPercentage() != null
                ? variant.getDiscountPercentage().setScale(0, RoundingMode.HALF_UP).intValue()
                : resolveDiscountPercent(variant, priceWithGst, mrp);

        String videoUrl = variant.getVideoPath() != null && !variant.getVideoPath().isBlank()
                ? resolveImageUrl(variant.getVideoPath())
                : null;

        return ProductVariantDetailResponse.builder()
                .id(variant.getId())
                .productId(variant.getProductId())
                .color(colorName)
                .colorHex(colorHex)
                .size(resolveSizeName(variant.getSize(), sizeById))
                .sku(firstNonBlank(variant.getSku(), "—"))
                .stock(variant.getStock() != null ? variant.getStock() : 0)
                .basePrice(variant.getBasePrice())
                .mrpExclGst(mrpExclGst)
                .mrpPrice(mrpPriceDb)
                .discountPercentage(variant.getDiscountPercentage())
                .discountAmount(variant.getDiscountAmount())
                .sellingPrice(variant.getSellingPrice())
                .taxPercentage(taxPct)
                .taxAmount(taxAmt)
                .finalPrice(variant.getFinalPrice())
                .mrpInclGst(mrpInclGstVal)
                .intraCityDeliveryCharge(intraCity)
                .metroMetroDeliveryCharge(metroMetro)
                .totalPriceIntraCity(totalIntra)
                .totalPriceMetroMetro(totalMetro)
                .commissionPercentage(commissionPct)
                .commissionAmount(commissionAmt)
                .videoPath(variant.getVideoPath())
                .weight(variant.getWeight())
                .createdAt(formatDate(variant.getCreatedAt()))
                .updatedAt(formatDate(variant.getUpdatedAt()))
                .mrp(mrp)
                .discount(discount)
                .sellingPriceExGst(sellingPriceExGst != null ? sellingPriceExGst : BigDecimal.ZERO)
                .gstPercent(taxPct)
                .gstAmount(taxAmt)
                .sellingPriceWithGst(priceWithGst)
                .commissionPercent(commissionPct)
                .intraCityDelivery(intraCity)
                .metroMetroDelivery(metroMetro)
                .totalIntraCity(totalIntra)
                .totalMetroMetro(totalMetro)
                .imageUri(resolveVariantImage(images, variant.getId()))
                .videoUri(videoUrl)
                .build();
    }

    private ProductVariant pickPrimaryVariant(List<ProductVariant> variants) {
        if (variants.isEmpty()) {
            return null;
        }
        return variants.stream()
                .sorted(java.util.Comparator
                        .comparing((ProductVariant v) -> v.getStock() != null && v.getStock() > 0).reversed()
                        .thenComparing(ProductVariant::getId))
                .findFirst()
                .orElse(variants.get(0));
    }

    private BigDecimal resolveSellingPrice(ProductVariant variant) {
        if (variant == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal value = firstDecimal(variant.getFinalPrice(), variant.getMrpPrice());
        return value != null ? value : BigDecimal.ZERO;
    }

    /** Customer-facing list/detail price — total metro-metro (selling + commission + delivery). */
    private BigDecimal resolveTotalMetroMetro(ProductVariant variant) {
        if (variant == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalMetro = firstDecimal(variant.getTotalPriceMetroMetro());
        if (totalMetro != null && totalMetro.compareTo(BigDecimal.ZERO) > 0) {
            return totalMetro;
        }
        return resolveSellingPrice(variant);
    }

    /** MRP shown with strikethrough — always from mrp_excl_gst (fallback base_price). */
    private BigDecimal resolveMrpExclGst(ProductVariant variant) {
        if (variant == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal value = firstDecimal(variant.getMrpExclGst(), variant.getBasePrice());
        return value != null ? value : BigDecimal.ZERO;
    }

    private int resolveDiscountPercent(ProductVariant variant, BigDecimal price, BigDecimal mrp) {
        if (variant != null && variant.getDiscountPercentage() != null) {
            return variant.getDiscountPercentage().setScale(0, RoundingMode.HALF_UP).intValue();
        }
        if (mrp == null || price == null || mrp.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        BigDecimal saved = mrp.subtract(price);
        if (saved.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return saved.multiply(BigDecimal.valueOf(100))
                .divide(mrp, 0, RoundingMode.HALF_UP)
                .intValue();
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

    private String resolveVariantImage(List<ProductImage> images, Long variantId) {
        return images.stream()
                .filter(img -> variantId.equals(img.getVariantId()))
                .findFirst()
                .map(img -> resolveImageUrl(img.getImagePath()))
                .orElse(images.stream()
                        .findFirst()
                        .map(img -> resolveImageUrl(img.getImagePath()))
                        .orElse(""));
    }

    private List<ProductSpecResponse> parseSpecifications(String json) {
        List<Map<String, Object>> rows = ProductSpecificationsCodec.parseSpecRowsForDisplay(json);
        List<ProductSpecResponse> specs = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String name = stringValue(row.get("name"));
            String value = stringValue(row.get("value"));
            if (!name.isBlank() && !value.isBlank()) {
                specs.add(ProductSpecResponse.builder().label(name).value(value).build());
            }
        }
        return specs;
    }

    @SuppressWarnings("unused")
    private List<ProductSpecResponse> parseSpecificationsLegacy(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<Map<String, Object>> rows = objectMapper.readValue(json, new TypeReference<>() {});
            List<ProductSpecResponse> specs = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                String name = stringValue(row.get("name"));
                String value = stringValue(row.get("value"));
                if (!name.isBlank() && !value.isBlank()) {
                    specs.add(ProductSpecResponse.builder().label(name).value(value).build());
                }
            }
            return specs;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<String> parseFeatures(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<ProductSizeChartRowResponse> parseSizeChart(SizeChart sizeChart) {
        if (sizeChart == null || sizeChart.getChartData() == null || sizeChart.getChartData().isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(sizeChart.getChartData());
            JsonNode measurements = root.path("measurements");
            if (measurements.isArray() && !measurements.isEmpty() && measurements.get(0).has("size_name")) {
                List<ProductSizeChartRowResponse> rows = new ArrayList<>();
                for (JsonNode row : measurements) {
                    rows.add(ProductSizeChartRowResponse.builder()
                            .size(row.path("size_name").asText("—"))
                            .chest(row.path("chest_bust").asText("—"))
                            .waist(row.path("waist").asText("—"))
                            .hip(row.path("hip").asText("—"))
                            .length(row.path("length").asText("—"))
                            .sleeve(row.path("sleeve").asText(""))
                            .build());
                }
                return rows;
            }

            JsonNode sizes = root.path("sizes");
            if (sizes.isArray()) {
                List<ProductSizeChartRowResponse> rows = new ArrayList<>();
                for (JsonNode row : sizes) {
                    rows.add(ProductSizeChartRowResponse.builder()
                            .size(row.path("size").asText("—"))
                            .chest(row.path("chest").asText("—"))
                            .waist(row.path("waist").asText("—"))
                            .hip(row.path("hip").asText("—"))
                            .length(row.path("length").asText("—"))
                            .sleeve(row.path("sleeve").asText(""))
                            .build());
                }
                return rows;
            }
            return List.of();
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<ProductDeliveryChargeResponse> buildDeliveryCharges(Product product) {
        List<ProductDeliveryChargeResponse> charges = new ArrayList<>();
        if (product.getIntraCityCharge() != null) {
            charges.add(ProductDeliveryChargeResponse.builder()
                    .zone("Intra City")
                    .standard(formatCurrency(product.getIntraCityCharge()))
                    .express("—")
                    .build());
        }
        if (product.getMetroMetroCharge() != null) {
            charges.add(ProductDeliveryChargeResponse.builder()
                    .zone("Metro to Metro")
                    .standard(formatCurrency(product.getMetroMetroCharge()))
                    .express("—")
                    .build());
        }
        return charges.isEmpty() ? List.of(
                ProductDeliveryChargeResponse.builder().zone("Standard").standard("—").express("—").build()
        ) : charges;
    }

    private ProductReturnDetailsResponse buildReturnDetails(Product product) {
        String policy = firstNonBlank(product.getReturnPolicy(), "—");
        return ProductReturnDetailsResponse.builder()
                .window(policy)
                .conditions(policy.equals("—") ? List.of() : List.of(policy))
                .process("Raise return request → Schedule pickup → Refund after approval")
                .refundMode("Original Payment Method / Wallet Credit")
                .build();
    }

    private String formatDeliveryEstimate(Integer min, Integer max) {
        if (min != null && max != null) {
            return min + "–" + max + " Business Days";
        }
        if (min != null) {
            return min + "+ Business Days";
        }
        if (max != null) {
            return "Up to " + max + " Business Days";
        }
        return "—";
    }

    private String formatGst(BigDecimal gst) {
        if (gst == null) {
            return "—";
        }
        return gst.stripTrailingZeros().toPlainString() + "%";
    }

    private String formatCurrency(BigDecimal amount) {
        return "₹" + amount.stripTrailingZeros().toPlainString();
    }

    private String formatWeight(BigDecimal weight) {
        if (weight == null) {
            return "—";
        }
        return weight.stripTrailingZeros().toPlainString() + " kg";
    }

    private String formatDimensions(Product product) {
        if (product.getLengthCm() == null && product.getWidthCm() == null && product.getHeightCm() == null) {
            return "—";
        }
        return String.format(
                Locale.ENGLISH,
                "%s × %s × %s cm",
                dashOrValue(product.getLengthCm()),
                dashOrValue(product.getWidthCm()),
                dashOrValue(product.getHeightCm()));
    }

    private String dashOrValue(BigDecimal value) {
        return value == null ? "—" : value.stripTrailingZeros().toPlainString();
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "—";
        }
        return dateTime.format(DISPLAY_DATE);
    }

    private String stripHtml(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return html.replaceAll("<[^>]*>", " ")
                .replace("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String resolveImageUrl(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        String base = mediaBaseUrl == null ? "" : mediaBaseUrl.trim();
        if (base.isEmpty()) {
            return path.startsWith("/") ? path : "/" + path;
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return path.startsWith("/") ? base + path : base + "/" + path;
    }

    private BigDecimal firstDecimal(BigDecimal... values) {
        for (BigDecimal value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}

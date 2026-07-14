package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.Enum.DeliveryType;
import com.ecommerce.authdemo.entity.Category;
import com.ecommerce.authdemo.entity.Product;
import com.ecommerce.authdemo.entity.ProductVariant;
import com.ecommerce.authdemo.entity.SubCategory;
import com.ecommerce.authdemo.repository.CategoryRepository;
import com.ecommerce.authdemo.repository.SubCategoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Customer-facing price — same stack as admin displayPrice:
 * sellingPrice (ex-GST) + GST + commission + highest delivery charge.
 * Cart may pass a concrete {@link DeliveryType} to use that zone's delivery instead of max().
 */
@Service
@RequiredArgsConstructor
public class CustomerPriceResolver {

    private static final BigDecimal DEFAULT_GST = new BigDecimal("5.00");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final PlatformIntegrationSettings integrationSettings;
    private final SubCategoryRepository subCategoryRepository;
    private final CategoryRepository categoryRepository;

    public record ResolvedPrice(
            BigDecimal sellingExclGst,
            BigDecimal gstPercent,
            BigDecimal taxAmount,
            BigDecimal priceAfterGst,
            BigDecimal commissionPercent,
            BigDecimal commissionAmount,
            BigDecimal priceBeforeDelivery,
            BigDecimal intraCityDeliveryCharge,
            BigDecimal metroMetroDeliveryCharge,
            DeliveryType deliveryType,
            BigDecimal deliveryCharge,
            BigDecimal customerPrice,
            BigDecimal totalPriceIntraCity,
            BigDecimal totalPriceMetroMetro) {}

    /** Catalog / wishlist / activity: admin-aligned total (highest delivery). */
    public ResolvedPrice resolve(Product product, ProductVariant variant) {
        return resolve(product, variant, null);
    }

    /**
     * @param deliveryType when null, uses highest of Intra-City / Metro-Metro (admin Total Price).
     *                     when set, uses that zone's delivery (cart / checkout).
     */
    public ResolvedPrice resolve(Product product, ProductVariant variant, DeliveryType deliveryType) {
        if (variant == null) {
            return null;
        }

        BigDecimal sellingExcl = firstPositive(variant.getSellingPrice(), variant.getBasePrice());
        if (sellingExcl == null) {
            BigDecimal fallback = firstPositive(variant.getFinalPrice(), variant.getBasePrice());
            if (fallback == null) {
                return null;
            }
            sellingExcl = fallback;
        }

        BigDecimal gstPercent = resolveGstPercent(product, variant);

        BigDecimal taxAmount;
        BigDecimal priceAfterGst;
        if (variant.getFinalPrice() != null
                && variant.getFinalPrice().compareTo(BigDecimal.ZERO) > 0) {
            priceAfterGst = variant.getFinalPrice().setScale(2, RoundingMode.HALF_UP);
            taxAmount = variant.getTaxAmount() != null && variant.getTaxAmount().compareTo(BigDecimal.ZERO) > 0
                    ? variant.getTaxAmount().setScale(2, RoundingMode.HALF_UP)
                    : sellingExcl.multiply(gstPercent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        } else {
            taxAmount = sellingExcl.multiply(gstPercent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            priceAfterGst = sellingExcl.add(taxAmount).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal commissionPercent = resolveCommissionPercent(variant);
        BigDecimal commissionAmount;
        if (variant.getCommissionAmount() != null
                && variant.getCommissionAmount().compareTo(BigDecimal.ZERO) > 0
                && variant.getCommissionPercentage() != null
                && variant.getCommissionPercentage().compareTo(BigDecimal.ZERO) > 0) {
            commissionAmount = variant.getCommissionAmount().setScale(2, RoundingMode.HALF_UP);
            commissionPercent = variant.getCommissionPercentage();
        } else {
            commissionAmount = priceAfterGst
                    .multiply(commissionPercent)
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        }

        BigDecimal priceBeforeDelivery = priceAfterGst.add(commissionAmount).setScale(2, RoundingMode.HALF_UP);

        BigDecimal intraDelivery = nonNegative(variant.getIntraCityDeliveryCharge());
        BigDecimal metroDelivery = nonNegative(variant.getMetroMetroDeliveryCharge());
        BigDecimal highestDelivery = intraDelivery.max(metroDelivery);

        // Always recompute like admin enrich() — do not trust stale DB totals (often missing commission).
        BigDecimal totalIntra = priceBeforeDelivery.add(intraDelivery).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalMetro = priceBeforeDelivery.add(metroDelivery).setScale(2, RoundingMode.HALF_UP);
        BigDecimal displayPrice = priceBeforeDelivery.add(highestDelivery).setScale(2, RoundingMode.HALF_UP);

        DeliveryType effectiveType;
        BigDecimal deliveryCharge;
        BigDecimal customerPrice;
        if (deliveryType == null) {
            effectiveType = metroDelivery.compareTo(intraDelivery) >= 0
                    ? DeliveryType.metro_metro
                    : DeliveryType.intra_city;
            deliveryCharge = highestDelivery;
            customerPrice = displayPrice;
        } else {
            effectiveType = deliveryType;
            deliveryCharge = effectiveType == DeliveryType.intra_city ? intraDelivery : metroDelivery;
            customerPrice = effectiveType == DeliveryType.intra_city ? totalIntra : totalMetro;
        }

        return new ResolvedPrice(
                sellingExcl,
                gstPercent,
                taxAmount,
                priceAfterGst,
                commissionPercent,
                commissionAmount,
                priceBeforeDelivery,
                intraDelivery,
                metroDelivery,
                effectiveType,
                deliveryCharge,
                customerPrice,
                totalIntra,
                totalMetro);
    }

    public BigDecimal resolveCustomerUnitPrice(Product product, ProductVariant variant) {
        return resolveCustomerUnitPrice(product, variant, null);
    }

    public BigDecimal resolveCustomerUnitPrice(Product product, ProductVariant variant, DeliveryType deliveryType) {
        ResolvedPrice resolved = resolve(product, variant, deliveryType);
        return resolved != null ? resolved.customerPrice() : null;
    }

    /**
     * Admin panel strike MRP for the user app: {@code mrp_excl_gst} only.
     * Does not add GST, commission, or delivery — selling/customer price stays unchanged.
     */
    public BigDecimal resolveCustomerStrikeMrp(ProductVariant variant, BigDecimal customerUnitPrice) {
        if (variant == null) {
            return null;
        }
        BigDecimal mrpExcl = firstPositive(variant.getMrpExclGst());
        if (mrpExcl != null) {
            return mrpExcl.setScale(2, RoundingMode.HALF_UP);
        }
        // Legacy / incomplete rows: never invent fees on top of MRP.
        return variant.resolveMrpUnitPrice();
    }

    public BigDecimal resolveDeliveryCharge(ProductVariant variant, DeliveryType deliveryType) {
        if (variant == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal intra = nonNegative(variant.getIntraCityDeliveryCharge());
        BigDecimal metro = nonNegative(variant.getMetroMetroDeliveryCharge());
        if (deliveryType == null) {
            return intra.max(metro);
        }
        return deliveryType == DeliveryType.intra_city ? intra : metro;
    }

    private BigDecimal resolveGstPercent(Product product, ProductVariant variant) {
        if (variant.getTaxPercentage() != null && variant.getTaxPercentage().compareTo(BigDecimal.ZERO) > 0) {
            return variant.getTaxPercentage();
        }
        if (product != null
                && product.getGstPercentage() != null
                && product.getGstPercentage().compareTo(BigDecimal.ZERO) > 0) {
            return product.getGstPercentage();
        }
        if (product != null && product.getSubcategoryId() != null) {
            SubCategory subCategory = subCategoryRepository
                    .findById(product.getSubcategoryId().longValue())
                    .orElse(null);
            if (subCategory != null) {
                Double fromSlabs = resolveGstFromMaterialSlabs(subCategory.getMaterialSlabs());
                if (fromSlabs != null) {
                    return BigDecimal.valueOf(fromSlabs);
                }
                if (subCategory.getGstPercentage() != null
                        && subCategory.getGstPercentage().compareTo(BigDecimal.ZERO) > 0) {
                    return subCategory.getGstPercentage();
                }
                if (subCategory.getCategoryId() != null) {
                    Category category = categoryRepository.findById(subCategory.getCategoryId()).orElse(null);
                    if (category != null && category.getGstPercentage() != null) {
                        return BigDecimal.valueOf(category.getGstPercentage());
                    }
                }
            }
        }
        if (product != null && product.getCategoryId() != null) {
            Category category = categoryRepository.findById(product.getCategoryId().longValue()).orElse(null);
            if (category != null && category.getGstPercentage() != null) {
                return BigDecimal.valueOf(category.getGstPercentage());
            }
        }
        return DEFAULT_GST;
    }

    private Double resolveGstFromMaterialSlabs(String materialSlabsRaw) {
        String normalized = normalize(materialSlabsRaw);
        if (normalized == null) {
            return null;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(normalized);
            return findNumberByKeys(root, "gstPercentage", "gst_percentage", "taxPercentage", "tax_percentage", "gst", "tax");
        } catch (Exception ignored) {
            return null;
        }
    }

    private Double findNumberByKeys(JsonNode node, String... keys) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            for (String key : keys) {
                JsonNode direct = node.get(key);
                if (direct != null && !direct.isNull() && direct.isNumber()) {
                    return direct.asDouble();
                }
            }
            for (JsonNode child : node) {
                Double found = findNumberByKeys(child, keys);
                if (found != null) {
                    return found;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                Double found = findNumberByKeys(child, keys);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private BigDecimal resolveCommissionPercent(ProductVariant variant) {
        if (variant.getCommissionPercentage() != null
                && variant.getCommissionPercentage().compareTo(BigDecimal.ZERO) > 0) {
            return variant.getCommissionPercentage();
        }
        return integrationSettings.getCommissionB2cPercent();
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static BigDecimal firstPositive(BigDecimal... candidates) {
        if (candidates == null) {
            return null;
        }
        for (BigDecimal candidate : candidates) {
            if (candidate != null && candidate.compareTo(BigDecimal.ZERO) > 0) {
                return candidate;
            }
        }
        return null;
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}

package com.ecommerce.authdemo.service;

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
 * Resolves customer-facing unit price:
 * sellingPrice (ex-GST) + GST (from category/HSN chain) + commission (from admin_settings).
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
            BigDecimal customerPrice) {}

    public ResolvedPrice resolve(Product product, ProductVariant variant) {
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

        BigDecimal customerPrice = priceAfterGst.add(commissionAmount).setScale(2, RoundingMode.HALF_UP);

        return new ResolvedPrice(
                sellingExcl,
                gstPercent,
                taxAmount,
                priceAfterGst,
                commissionPercent,
                commissionAmount,
                customerPrice);
    }

    public BigDecimal resolveCustomerUnitPrice(Product product, ProductVariant variant) {
        ResolvedPrice resolved = resolve(product, variant);
        return resolved != null ? resolved.customerPrice() : null;
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
}

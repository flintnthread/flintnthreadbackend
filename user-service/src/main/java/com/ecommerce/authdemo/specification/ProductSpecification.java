package com.ecommerce.authdemo.specification;

import com.ecommerce.authdemo.dto.EnhancedProductFilterRequestDTO;
import com.ecommerce.authdemo.entity.Product;
import com.ecommerce.authdemo.entity.ProductVariant;
import com.ecommerce.authdemo.entity.Category;
import com.ecommerce.authdemo.util.ProductCatalogVisibility;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ProductSpecification {

    public static Specification<Product> filterProducts(EnhancedProductFilterRequestDTO request) {

        return (Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Only ACTIVE (admin-approved) products
            predicates.add(cb.equal(cb.lower(root.get("status")), ProductCatalogVisibility.USER_VISIBLE_STATUS));

            // Keyword search in name and description
            if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
                String keyword = "%" + request.getKeyword().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), keyword),
                        cb.like(cb.lower(root.get("shortDescription")), keyword),
                        cb.like(cb.lower(root.get("description")), keyword)
                ));
            }

            // Category filters - support multiple categories
            if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
                predicates.add(root.get("categoryId").in(request.getCategoryIds()));
            }

            // Main department filters (Women / Men / Kids + all subcategories)
            if (request.getMainCategoryIds() != null && !request.getMainCategoryIds().isEmpty()) {
                Subquery<Long> subcategoryIds = query.subquery(Long.class);
                Root<Category> categoryRoot = subcategoryIds.from(Category.class);
                subcategoryIds.select(categoryRoot.get("id"));
                subcategoryIds.where(categoryRoot.get("parentId").in(request.getMainCategoryIds()));

                predicates.add(cb.or(
                        root.get("categoryId").in(request.getMainCategoryIds()),
                        root.get("categoryId").in(subcategoryIds)
                ));
            }

            if (request.getSubcategoryIds() != null && !request.getSubcategoryIds().isEmpty()) {
                predicates.add(root.get("subcategoryId").in(request.getSubcategoryIds()));
            }

            // Seller filter
            if (request.getSellerId() != null) {
                predicates.add(cb.equal(root.get("sellerId"), request.getSellerId()));
            }

            // Gender filter (case-insensitive)
            if (request.getGenders() != null && !request.getGenders().isEmpty()) {
                List<String> normalizedGenders = request.getGenders().stream()
                        .filter(g -> g != null && !g.isBlank())
                        .map(g -> g.trim().toLowerCase())
                        .distinct()
                        .toList();
                if (!normalizedGenders.isEmpty()) {
                    predicates.add(cb.lower(root.get("gender")).in(normalizedGenders));
                }
            }

            // Price range filter - Join with ProductVariant
            if (request.getMinPrice() != null || request.getMaxPrice() != null) {
                Join<Product, ProductVariant> variantJoin = root.join("variants", JoinType.INNER);

                if (request.getMinPrice() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(
                            variantJoin.get("sellingPrice"), request.getMinPrice()
                    ));
                }

                if (request.getMaxPrice() != null) {
                    predicates.add(cb.lessThanOrEqualTo(
                            variantJoin.get("sellingPrice"), request.getMaxPrice()
                    ));
                }
            }

            // Color filter — variants store color id (e.g. "1") and/or free-text name ("Red")
            List<String> colorTokens = collectColorTokens(request);
            if (!colorTokens.isEmpty()) {
                Join<Product, ProductVariant> variantJoin = root.join("variants", JoinType.INNER);
                predicates.add(matchVariantAttribute(cb, variantJoin.get("color"), colorTokens));
            }

            // Size filter — variants store size id and/or free-text name
            List<String> sizeTokens = collectSizeTokens(request);
            if (!sizeTokens.isEmpty()) {
                Join<Product, ProductVariant> variantJoin = root.join("variants", JoinType.INNER);
                predicates.add(matchVariantAttribute(cb, variantJoin.get("size"), sizeTokens));
            }

            // Stock filter
            if (request.getInStock() != null && request.getInStock()) {
                Join<Product, ProductVariant> variantJoin = root.join("variants", JoinType.INNER);
                predicates.add(cb.greaterThan(variantJoin.get("stock"), 0));
            }

            // Rating filter - Use product's rating field
            if (request.getMinRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), request.getMinRating()));
            }

            // Remove duplicates when joining with variants
            query.distinct(true);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Legacy method for old ProductFilterRequestDTO
    public static Specification<Product> filterProductsLegacy(com.ecommerce.authdemo.dto.ProductFilterRequestDTO request) {

        return (Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Only ACTIVE (admin-approved) products
            predicates.add(cb.equal(cb.lower(root.get("status")), ProductCatalogVisibility.USER_VISIBLE_STATUS));

            // Keyword search in name and description
            if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
                String keyword = "%" + request.getKeyword().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), keyword),
                        cb.like(cb.lower(root.get("shortDescription")), keyword),
                        cb.like(cb.lower(root.get("description")), keyword)
                ));
            }

            // Category filters
            if (request.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("categoryId"), request.getCategoryId()));
            }

            if (request.getSubcategoryId() != null) {
                predicates.add(cb.equal(root.get("subcategoryId"), request.getSubcategoryId()));
            }

            // Seller filter
            if (request.getSellerId() != null) {
                predicates.add(cb.equal(root.get("sellerId"), request.getSellerId()));
            }

            // Gender filter (case-insensitive)
            if (request.getGenders() != null && !request.getGenders().isEmpty()) {
                List<String> normalizedGenders = request.getGenders().stream()
                        .filter(g -> g != null && !g.isBlank())
                        .map(g -> g.trim().toLowerCase())
                        .distinct()
                        .toList();
                if (!normalizedGenders.isEmpty()) {
                    predicates.add(cb.lower(root.get("gender")).in(normalizedGenders));
                }
            }

            // Price range filter - Join with ProductVariant
            if (request.getMinPrice() != null || request.getMaxPrice() != null) {
                Join<Product, ProductVariant> variantJoin = root.join("variants", JoinType.INNER);

                if (request.getMinPrice() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(
                            variantJoin.get("sellingPrice"), request.getMinPrice()
                    ));
                }

                if (request.getMaxPrice() != null) {
                    predicates.add(cb.lessThanOrEqualTo(
                            variantJoin.get("sellingPrice"), request.getMaxPrice()
                    ));
                }
            }

            // Color filter — match id and/or name tokens on variants
            if (request.getColors() != null && !request.getColors().isEmpty()) {
                Join<Product, ProductVariant> variantJoin = root.join("variants", JoinType.INNER);
                predicates.add(matchVariantAttribute(cb, variantJoin.get("color"), request.getColors()));
            }

            // Size filter — match id and/or name tokens on variants
            if (request.getSizes() != null && !request.getSizes().isEmpty()) {
                Join<Product, ProductVariant> variantJoin = root.join("variants", JoinType.INNER);
                predicates.add(matchVariantAttribute(cb, variantJoin.get("size"), request.getSizes()));
            }

            // Stock filter
            if (request.getInStock() != null && request.getInStock()) {
                Join<Product, ProductVariant> variantJoin = root.join("variants", JoinType.INNER);
                predicates.add(cb.greaterThan(variantJoin.get("stock"), 0));
            }

            // Rating filter - Use product's rating field
            if (request.getMinRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), request.getMinRating()));
            }

            // Remove duplicates when joining with variants
            query.distinct(true);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static List<String> collectColorTokens(EnhancedProductFilterRequestDTO request) {
        Set<String> tokens = new LinkedHashSet<>();
        if (request.getColorIds() != null) {
            for (Long id : request.getColorIds()) {
                if (id != null && id > 0) {
                    tokens.add(String.valueOf(id));
                }
            }
        }
        if (request.getColorNames() != null) {
            for (String name : request.getColorNames()) {
                if (name != null && !name.isBlank()) {
                    tokens.add(name.trim());
                }
            }
        }
        return new ArrayList<>(tokens);
    }

    private static List<String> collectSizeTokens(EnhancedProductFilterRequestDTO request) {
        Set<String> tokens = new LinkedHashSet<>();
        if (request.getSizeIds() != null) {
            for (Long id : request.getSizeIds()) {
                if (id != null && id > 0) {
                    tokens.add(String.valueOf(id));
                }
            }
        }
        if (request.getSizeNames() != null) {
            for (String name : request.getSizeNames()) {
                if (name != null && !name.isBlank()) {
                    tokens.add(name.trim());
                }
            }
        }
        return new ArrayList<>(tokens);
    }

    /** Match variant.color / variant.size against id strings and names (case-insensitive). */
    private static Predicate matchVariantAttribute(
            CriteriaBuilder cb,
            Path<String> attribute,
            List<String> tokens
    ) {
        Set<String> exact = new LinkedHashSet<>();
        Set<String> lowered = new LinkedHashSet<>();
        for (String token : tokens) {
            if (token == null || token.isBlank()) continue;
            String trimmed = token.trim();
            exact.add(trimmed);
            lowered.add(trimmed.toLowerCase(Locale.ROOT));
        }
        if (exact.isEmpty()) {
            return cb.disjunction();
        }
        return cb.or(
                attribute.in(exact),
                cb.lower(attribute).in(lowered)
        );
    }
}

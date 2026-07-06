package com.ecommerce.authdemo.specification;

import com.ecommerce.authdemo.dto.EnhancedProductFilterRequestDTO;
import com.ecommerce.authdemo.entity.Product;
import com.ecommerce.authdemo.entity.ProductVariant;
import com.ecommerce.authdemo.entity.ProductColor;
import com.ecommerce.authdemo.entity.ProductSize;
import com.ecommerce.authdemo.entity.Category;
import com.ecommerce.authdemo.util.ProductCatalogVisibility;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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

            // Color filter - Use new many-to-many relationship
            if ((request.getColorIds() != null && !request.getColorIds().isEmpty()) ||
                    (request.getColorNames() != null && !request.getColorNames().isEmpty())) {

                Join<Product, ProductColor> colorJoin = root.join("productColors", JoinType.INNER);

                if (request.getColorIds() != null && !request.getColorIds().isEmpty()) {
                    predicates.add(colorJoin.get("color").get("id").in(request.getColorIds()));
                }

                if (request.getColorNames() != null && !request.getColorNames().isEmpty()) {
                    predicates.add(colorJoin.get("color").get("name").in(request.getColorNames()));
                }
            }

            // Size filter - Use new many-to-many relationship
            if ((request.getSizeIds() != null && !request.getSizeIds().isEmpty()) ||
                    (request.getSizeNames() != null && !request.getSizeNames().isEmpty())) {

                Join<Product, ProductSize> sizeJoin = root.join("productSizes", JoinType.INNER);

                if (request.getSizeIds() != null && !request.getSizeIds().isEmpty()) {
                    predicates.add(sizeJoin.get("size").get("id").in(request.getSizeIds()));
                }

                if (request.getSizeNames() != null && !request.getSizeNames().isEmpty()) {
                    predicates.add(sizeJoin.get("size").get("name").in(request.getSizeNames()));
                }
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

            // Color filter - Join with ProductVariant
            if (request.getColors() != null && !request.getColors().isEmpty()) {
                Join<Product, ProductVariant> variantJoin = root.join("variants", JoinType.INNER);
                predicates.add(variantJoin.get("color").in(request.getColors()));
            }

            // Size filter - Join with ProductVariant
            if (request.getSizes() != null && !request.getSizes().isEmpty()) {
                Join<Product, ProductVariant> variantJoin = root.join("variants", JoinType.INNER);
                predicates.add(variantJoin.get("size").in(request.getSizes()));
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
}
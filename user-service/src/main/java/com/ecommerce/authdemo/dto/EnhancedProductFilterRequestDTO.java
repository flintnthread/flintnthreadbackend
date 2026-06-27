package com.ecommerce.authdemo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class EnhancedProductFilterRequestDTO {

    // 🔎 Search
    private String keyword;

    // 📄 Pagination
    private int page = 0;
    private int size = 20;

    // 🔃 Sorting
    private String sortBy = "createdAt";
    private String sortDirection = "desc";

    // 📂 Category Filters
    private List<Long> categoryIds;
    private List<Long> subcategoryIds;
    /** Main department ids (Women / Men / Kids) — includes all subcategories under each. */
    private List<Long> mainCategoryIds;

    // 👕 Gender Filter
    private List<String> genders;

    // 🎨 Color Filter
    private List<Long> colorIds;
    private List<String> colorNames;

    // 📏 Size Filter
    private List<Long> sizeIds;
    private List<String> sizeNames;

    // 💰 Price Range
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    // ⭐ Rating Filter
    private Double minRating;

    // 🏪 Seller Filter
    private Long sellerId;

    // 📦 Stock Filter
    private Boolean inStock = true;

    // 🚚 Delivery Filters
    private Boolean acceptCod;
    private Boolean acceptPrepaid;

    // 📍 Location Filters
    private Boolean deliverAllLocations;
}

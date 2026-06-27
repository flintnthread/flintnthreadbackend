package com.ecommerce.authdemo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductFilterRequestDTO {

    // 🔎 Search
    private String keyword;

    // 📄 Pagination
    private int page = 0;
    private int size = 10;

    // 🔃 Sorting
    private String sortBy = "createdAt";
    private String sortDirection = "desc";

    // 📂 Category Filters
    private Long categoryId;
    private Long subcategoryId;
    /** Main department id (Women / Men / Kids) — includes subcategories. */
    private Long mainCategoryId;

    // 💰 Price Range
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    // ⭐ Rating Filter
    private Double minRating;

    // 👕 Product Attributes
    private List<String> genders;
    private List<String> fabrics;

    // 🎨 Variant Filters
    private List<String> colors;
    private List<String> sizes;

    // 🏪 Seller Filter
    private Long sellerId;

    // 📦 Stock Filter
    private Boolean inStock;

}
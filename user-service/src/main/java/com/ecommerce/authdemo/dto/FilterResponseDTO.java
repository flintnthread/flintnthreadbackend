package com.ecommerce.authdemo.dto;

import lombok.Data;

import java.util.List;

@Data
public class FilterResponseDTO {
    
    private List<ProductDTO> products;
    private long totalProducts;
    private int currentPage;
    private int totalPages;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;
    
    // Applied filters summary
    private AppliedFiltersSummary appliedFilters;
    
    @Data
    public static class AppliedFiltersSummary {
        private List<String> categories;
        private List<String> colors;
        private List<String> sizes;
        private List<String> genders;
        private String priceRange;
        private Double minRating;
        private int totalActiveFilters;
    }
}

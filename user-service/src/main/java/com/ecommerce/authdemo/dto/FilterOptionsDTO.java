package com.ecommerce.authdemo.dto;

import lombok.Data;

import java.util.List;

@Data
public class FilterOptionsDTO {
    
    private List<CategoryFilterDTO> categories;
    private List<ColorFilterDTO> colors;
    private List<SizeFilterDTO> sizes;
    private List<GenderFilterDTO> genders;
    private List<PriceRangeDTO> priceRanges;
    private List<RatingFilterDTO> ratings;
    
    @Data
    public static class CategoryFilterDTO {
        private Long id;
        private String name;
        private String image;
        private Long parentId;
        private Integer productCount;
    }
    
    @Data
    public static class ColorFilterDTO {
        private Long id;
        private String name;
        private String code;
        private String hex;
        private Integer productCount;
    }
    
    @Data
    public static class SizeFilterDTO {
        private Long id;
        private String name;
        private String code;
        private Integer productCount;
    }
    
    @Data
    public static class GenderFilterDTO {
        private String value;
        private String label;
        private Integer productCount;
    }
    
    @Data
    public static class PriceRangeDTO {
        private String id;
        private String label;
        private Double min;
        private Double max;
        private Integer productCount;
    }
    
    @Data
    public static class RatingFilterDTO {
        private Integer value;
        private String label;
        private Integer productCount;
    }
}

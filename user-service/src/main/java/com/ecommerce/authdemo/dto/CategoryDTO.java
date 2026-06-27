package com.ecommerce.authdemo.dto;

import lombok.Data;

    @Data
    public class CategoryDTO {

        private Long id;
        private Long parentId;
        private String categoryName;
        private String image;
        private String mobileImage;
        private String bannerImage;
        private String hsnCode;
        private Double gstPercentage;
        private Integer status;
        private Long sellerId;
    }


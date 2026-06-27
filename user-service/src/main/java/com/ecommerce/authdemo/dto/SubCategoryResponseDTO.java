package com.ecommerce.authdemo.dto;

import lombok.Data;

@Data
public class SubCategoryResponseDTO {

        private Long id;
        private String name;
        private String image;
       private String mobileImage; // ✅ ADD


    public SubCategoryResponseDTO(Long id, String name, String image, String mobileImage) {
            this.id = id;
            this.name = name;
            this.image = image;
        this.mobileImage = mobileImage;

    }

        // getters
    }


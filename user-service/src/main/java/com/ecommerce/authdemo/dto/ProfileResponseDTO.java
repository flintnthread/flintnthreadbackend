package com.ecommerce.authdemo.dto;

import lombok.Builder;
import lombok.Data;

@Data
    @Builder
    public class ProfileResponseDTO {

        private Long id;
        private String name;
        private String email;
        private String contactNumber;
        private String profileImage;

        private ShopperDTO activeShopper;
    }


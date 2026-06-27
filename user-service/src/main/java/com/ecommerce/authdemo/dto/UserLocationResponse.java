package com.ecommerce.authdemo.dto;

import lombok.*;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public class UserLocationResponse {

        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String country;
        private String pincode;
    }


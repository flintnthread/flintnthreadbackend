package com.ecommerce.authdemo.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
    @AllArgsConstructor
    public class ShopperDTO {

        private Integer id;
        private String name;
        private Boolean isActive;
    }


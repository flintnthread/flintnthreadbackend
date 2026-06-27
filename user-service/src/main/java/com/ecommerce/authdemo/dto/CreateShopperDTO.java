package com.ecommerce.authdemo.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
    public class CreateShopperDTO {

        @NotBlank
        private String name;
    }


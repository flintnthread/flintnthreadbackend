package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductSpecResponse {
    private String label;
    private String value;
}

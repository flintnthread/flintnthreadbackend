package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SalesTrendPointDto {
    private String label;
    private double value;
}

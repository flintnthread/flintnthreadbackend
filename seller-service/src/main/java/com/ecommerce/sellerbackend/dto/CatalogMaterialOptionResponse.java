package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CatalogMaterialOptionResponse {
    private String material;
    private String hsnCode;
    private BigDecimal gst;
}

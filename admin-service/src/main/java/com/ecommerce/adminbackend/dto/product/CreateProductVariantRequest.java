package com.ecommerce.adminbackend.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CreateProductVariantRequest {

    private String clientKey;

    private Long colorId;

    @NotBlank
    private String color;

    private Long sizeId;

    @NotBlank
    private String size;

    private String sku;

    @NotNull
    private Integer stock;

    @NotNull
    private BigDecimal mrp;

    @NotNull
    private BigDecimal sellingPrice;

    private BigDecimal discount;

    private String videoUrl;

    private List<CreateProductImageRequest> images = new ArrayList<>();
}

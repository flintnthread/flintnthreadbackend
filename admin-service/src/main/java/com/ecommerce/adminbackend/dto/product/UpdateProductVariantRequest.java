package com.ecommerce.adminbackend.dto.product;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProductVariantRequest extends CreateProductVariantRequest {

    private Long id;

    private Boolean remove;
}

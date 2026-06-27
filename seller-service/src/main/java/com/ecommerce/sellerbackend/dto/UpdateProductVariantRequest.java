package com.ecommerce.sellerbackend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProductVariantRequest extends CreateProductVariantRequest {

    /** Existing variant id; omit for new variants on update */
    private Long id;

    /** When true, variant is removed during product update */
    private Boolean remove;
}

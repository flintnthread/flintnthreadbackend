package com.ecommerce.adminbackend.dto.product;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateProductImageRequest {

    /** Local file URI, https URL, relative uploads path, or data:image/...;base64,... */
    private String source;

    private Boolean primary;

    private Integer sortOrder;

    /** Matches CreateProductVariantRequest.clientKey when image belongs to a variant */
    private String variantClientKey;
}

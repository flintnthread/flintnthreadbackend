package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CreateProductResponse {

    private Long productId;
    private List<CreatedVariantRef> variants;

    @Getter
    @Builder
    public static class CreatedVariantRef {
        private String clientKey;
        private Long variantId;
    }
}

package com.ecommerce.authdemo.dto;

import lombok.Data;




@Data
public class WishlistProductResponse {

    private Long wishlistId;

    private ProductDTO product;
}
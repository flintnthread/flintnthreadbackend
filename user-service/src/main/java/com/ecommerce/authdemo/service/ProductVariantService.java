package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.StockResponse;

public interface ProductVariantService {
    StockResponse getStockByVariantId(Long variantId);

    Object getVariantsByProductId(Long productId);



}

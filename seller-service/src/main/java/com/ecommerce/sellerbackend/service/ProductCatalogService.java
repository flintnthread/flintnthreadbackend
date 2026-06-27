package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.ProductFormCatalogResponse;

public interface ProductCatalogService {

    ProductFormCatalogResponse getProductFormCatalog(Long sellerId);
}

package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.ProductDTO;
import com.ecommerce.authdemo.dto.SellerProfileDTO;
import com.ecommerce.authdemo.dto.SellerReviewDTO;
import com.ecommerce.authdemo.dto.SellerStoreResponseDTO;
import org.springframework.data.domain.Page;

public interface SellerStoreService {
    SellerProfileDTO getProfile(Long sellerId);

    SellerStoreResponseDTO getStore(Long sellerId);

    Page<ProductDTO> getProducts(Long sellerId, int page, int size);

    Page<SellerReviewDTO> getReviews(Long sellerId, int page, int size);
}

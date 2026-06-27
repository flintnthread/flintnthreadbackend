package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.entity.Product;
import com.ecommerce.sellerbackend.exception.ResourceNotFoundException;
import com.ecommerce.sellerbackend.repository.ProductImageRepository;
import com.ecommerce.sellerbackend.repository.ProductPincodeRepository;
import com.ecommerce.sellerbackend.repository.ProductRepository;
import com.ecommerce.sellerbackend.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductDeleteService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductPincodeRepository productPincodeRepository;

    @Transactional
    public void deleteForSeller(Long sellerId, Long productId) {
        Product product = productRepository.findByIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));

        productImageRepository.deleteByProductId(productId);
        productVariantRepository.deleteByProductId(productId);
        productPincodeRepository.deleteByProductId(productId);

        try {
            productRepository.delete(product);
        } catch (DataIntegrityViolationException ex) {
            product.setStatus("inactive");
            productRepository.save(product);
        }
    }
}

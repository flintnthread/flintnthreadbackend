package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.StockResponse;
import com.ecommerce.authdemo.repository.ProductVariantRepository;
import com.ecommerce.authdemo.service.ProductVariantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductVariantServiceImpl implements ProductVariantService {

    @Autowired
    private ProductVariantRepository repository;

    @Override
    public StockResponse getStockByVariantId(Long variantId) {

        Integer stock = repository.findStockByVariantId(variantId)
                .orElseThrow(() -> new RuntimeException("Variant not found"));

        if (stock > 0) {
            return new StockResponse(variantId, stock, "Stock available");
        } else {
            return new StockResponse(variantId, 0, "Out of stock");
        }
    }


    @Override
    public Object getVariantsByProductId(Long productId) {

        return repository.findByProductId(productId);
    }
}
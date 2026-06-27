package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdOrderBySortOrderAsc(Long productId);

    ProductImage findTopByProductIdAndIsPrimaryTrue(Long productId);
    Optional<ProductImage> findFirstByProductId(Long productId);
}
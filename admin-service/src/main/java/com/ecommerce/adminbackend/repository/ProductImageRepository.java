package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdOrderBySortOrderAsc(Long productId);

    List<ProductImage> findByProductIdInOrderBySortOrderAsc(Collection<Long> productIds);
}

package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdInOrderByIsPrimaryDescSortOrderAsc(Collection<Long> productIds);

    List<ProductImage> findByProductId(Long productId);

    void deleteByProductId(Long productId);
}

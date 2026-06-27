package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductIdOrderByIdAsc(Long productId);
}

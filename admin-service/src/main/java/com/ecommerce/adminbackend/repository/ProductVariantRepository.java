package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductIdOrderByIdAsc(Long productId);

    List<ProductVariant> findBySizeIsNotNull();

    @org.springframework.data.jpa.repository.Query(value = """
            SELECT COUNT(DISTINCT p.id)
            FROM products p
            WHERE COALESCE((
                SELECT SUM(COALESCE(pv.stock, 0))
                FROM product_variants pv
                WHERE pv.product_id = p.id
            ), 0) <= 0
            """, nativeQuery = true)
    long countOutOfStockProducts();

    @org.springframework.data.jpa.repository.Query(value = """
            SELECT COUNT(DISTINCT p.id)
            FROM products p
            WHERE COALESCE((
                SELECT SUM(COALESCE(pv.stock, 0))
                FROM product_variants pv
                WHERE pv.product_id = p.id
            ), 0) BETWEEN 1 AND 10
            """, nativeQuery = true)
    long countLowStockProducts();
}
